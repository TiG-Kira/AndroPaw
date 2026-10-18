#!/usr/bin/env python3
import os
import sys
import subprocess
import glob

def find_ndk():
    candidates = [
        os.environ.get("ANDROID_NDK_HOME"),
        os.environ.get("NDK_HOME"),
    ]
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT") or os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk")
    if os.path.isdir(os.path.join(sdk, "ndk")):
        for d in sorted(os.listdir(os.path.join(sdk, "ndk")), reverse=True):
            candidates.append(os.path.join(sdk, "ndk", d))
    for c in candidates:
        if c and os.path.isdir(c):
            bin_dir = os.path.join(c, "toolchains", "llvm", "prebuilt", "windows-x86_64", "bin")
            if not os.path.isdir(bin_dir):
                bin_dir = os.path.join(c, "toolchains", "llvm", "prebuilt", "linux-x86_64", "bin")
            if os.path.isdir(bin_dir):
                return c, bin_dir
    raise RuntimeError("Could not find Android NDK")

def run(cmd, cwd=None):
    print(" ".join(cmd))
    res = subprocess.run(cmd, cwd=cwd)
    if res.returncode != 0:
        sys.exit(f"Failed with code {res.returncode}: {' '.join(cmd)}")

def main():
    ndk_dir, bin_dir = find_ndk()
    clang = os.path.join(bin_dir, "clang.exe" if os.name == "nt" else "clang")
    strip = os.path.join(bin_dir, "llvm-strip.exe" if os.name == "nt" else "llvm-strip")
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    assets = os.path.join(repo_root, "app", "src", "main", "assets", "engines")
    
    sysroot = os.path.join(ndk_dir, "toolchains", "llvm", "prebuilt", 
                           "windows-x86_64" if os.name == "nt" else "linux-x86_64", "sysroot")
    arm_lib = os.path.join(sysroot, "usr", "lib", "arm-linux-androideabi", "21")

    print(f"Using NDK Clang: {clang}")

    # 1. guestshim: libaemushim.so, aemu_true.so, librecshim.so
    shim_dir = os.path.join(repo_root, "native", "guestshim")
    common_dir = os.path.join(assets, "common")
    os.makedirs(common_dir, exist_ok=True)
    
    out_shim = os.path.join(common_dir, "libaemushim.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-fvisibility=hidden", "-fno-stack-protector", "-ffreestanding", "-fno-builtin",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--no-undefined", "-Wl,-soname,libaemushim.so",
         "-Wl,--build-id=none", "-o", out_shim, "aemushim.c", "crash.c", "-lc"], cwd=shim_dir)
    subprocess.run([strip, out_shim])

    out_true = os.path.join(common_dir, "aemu_true.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-nostdlib", "-static", "-Wl,--build-id=none",
         "-o", out_true, "true.S"], cwd=shim_dir)
    subprocess.run([strip, out_true])

    out_rec = os.path.join(common_dir, "librecshim.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-fvisibility=hidden", "-fno-stack-protector", "-ffreestanding", "-fno-builtin",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--no-undefined", "-Wl,-soname,librecshim.so",
         "-Wl,--build-id=none", "-o", out_rec, "recshim.c", "-lc"], cwd=shim_dir)
    subprocess.run([strip, out_rec])

    # ashmem: libashmemshim.so for GB and KK
    gb_dir = os.path.join(assets, "gb")
    kk_dir = os.path.join(assets, "kk")
    os.makedirs(gb_dir, exist_ok=True)
    os.makedirs(kk_dir, exist_ok=True)
    out_ashmem_kk = os.path.join(kk_dir, "libashmemshim.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-fvisibility=hidden", "-fno-stack-protector", "-ffreestanding", "-fno-builtin",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--no-undefined", "-Wl,-soname,libashmemshim.so",
         "-Wl,--build-id=none", "-o", out_ashmem_kk, "ashmem.c", "-lc"], cwd=shim_dir)
    subprocess.run([strip, out_ashmem_kk])
    import shutil
    shutil.copy2(out_ashmem_kk, os.path.join(gb_dir, "libashmemshim.so"))

    # 2. lightshal: lights.default.so
    lights_dir = os.path.join(repo_root, "native", "lightshal")
    out_lights = os.path.join(common_dir, "lights.default.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,--no-undefined",
         "-Wl,-soname,lights.default.so", "-o", out_lights, "lights_hal.c", "-lc"], cwd=lights_dir)
    subprocess.run([strip, out_lights])

    # 3. sensorshal: sensors.default.so
    sensors_dir = os.path.join(repo_root, "native", "sensorshal")
    out_sensors = os.path.join(common_dir, "sensors.default.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,--no-undefined",
         "-Wl,-soname,sensors.default.so", "-o", out_sensors, "sensors_hal.c", "-lc"], cwd=sensors_dir)
    subprocess.run([strip, out_sensors])

    # 4. camerahal: camera.default.so, libcamera.none.so
    camera_dir = os.path.join(repo_root, "native", "camerahal")
    out_camera = os.path.join(common_dir, "camera.default.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,--no-undefined",
         "-Wl,-soname,camera.default.so", "-o", out_camera, "camera_hal.c", "-ldl", "-lc"], cwd=camera_dir)
    subprocess.run([strip, out_camera])

    out_cam_none = os.path.join(common_dir, "libcamera.none.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,--no-undefined",
         "-Wl,-soname,libcamera.so", "-o", out_cam_none, "camera_gb_none.c"], cwd=camera_dir)
    subprocess.run([strip, out_cam_none])

    # 5. audiohal: audio.primary.*.so
    audio_dir = os.path.join(repo_root, "native", "audiohal")
    kk_dir = os.path.join(assets, "kk")
    os.makedirs(kk_dir, exist_ok=True)

    out_aosp = os.path.join(kk_dir, "audio.primary.aosp.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-mfloat-abi=softfp", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,-soname,audio.primary.default.so",
         "-o", out_aosp, "audio_hal.c", "-lc"], cwd=audio_dir)
    subprocess.run([strip, out_aosp])

    out_ics = os.path.join(kk_dir, "audio.primary.ics.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-mfloat-abi=softfp", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector", "-DAEMU_ICS",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,-soname,audio.primary.default.so",
         "-o", out_ics, "audio_hal.c", "-lc"], cwd=audio_dir)
    subprocess.run([strip, out_ics])

    out_ics_qcom = os.path.join(kk_dir, "audio.primary.ics-qcom.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-mfloat-abi=softfp", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector", "-DAEMU_ICS", "-DAEMU_QCOM",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,-soname,audio.primary.default.so",
         "-o", out_ics_qcom, "audio_hal.c", "-lc"], cwd=audio_dir)
    subprocess.run([strip, out_ics_qcom])

    out_mtk = os.path.join(kk_dir, "audio.primary.mtk.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-mfloat-abi=softfp", "-Os", "-fPIC", "-shared",
         "-nostdlib", "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector", "-DAEMU_MTK", '-DAEMU_PCM_DEV="/dev/aemu_pcm"',
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,-soname,libaudio.primary.default.so",
         "-o", out_mtk, "audio_hal.c", "-lc"], cwd=audio_dir)
    subprocess.run([strip, out_mtk])

    # 6. apwrap: audio_policy.wrap.so
    ap_dir = os.path.join(repo_root, "native", "apwrap")
    out_apwrap = os.path.join(kk_dir, "audio_policy.wrap.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-mthumb", "-Os", "-fPIC", "-shared", "-nostdlib",
         "-ffreestanding", "-fno-builtin", "-fvisibility=hidden", "-fno-stack-protector",
         "-Wl,--hash-style=sysv", "-Wl,-z,norelro", "-Wl,--build-id=none", "-Wl,-soname,audio_policy.default.so",
         "-o", out_apwrap, "apwrap.c", "tramp.S", "-ldl", "-lc", "-llog"], cwd=ap_dir)
    subprocess.run([strip, out_apwrap])

    # 7. glsplit: libGLES_split.so
    glsplit_dir = os.path.join(repo_root, "native", "glsplit")
    out_split = os.path.join(kk_dir, "libGLES_split.so")
    run([clang, "--target=armv7a-linux-androideabi21", "-march=armv7-a", "-Os", "-fPIC", "-shared", "-nostdlib",
         "-fvisibility=hidden", "-fno-stack-protector", "-ffreestanding", "-fno-builtin", "-Wl,--hash-style=sysv", "-Wl,-z,norelro",
         "-Wl,-soname,libGLES_split.so", "-Wl,--build-id=none", "-o", out_split, "split.c", "stubs.S",
         f"-L{arm_lib}", "-ldl", "-lc", "-llog"], cwd=glsplit_dir)
    subprocess.run([strip, out_split])

    print("=== All native guest components compiled successfully! ===")

if __name__ == "__main__":
    main()
