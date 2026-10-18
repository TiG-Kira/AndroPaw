/*
 * libcamera.so for Android 2.3 with no cameras. CameraService of 2.3 links the camera HAL directly (three C
 * functions); Qualcomm's HAL of that time reaches the modem over ONCRPC as soon as it is asked for the number of
 * cameras, cannot find it here and exits the whole mediaserver ("Failed to lookup RPC client"), taking sound with
 * it. This one answers "no cameras".
 */
#define EXPORT __attribute__((visibility("default")))

struct CameraInfo { int facing, orientation; };

EXPORT int HAL_getNumberOfCameras(void) { return 0; }

EXPORT void HAL_getCameraInfo(int id, struct CameraInfo *info) {
    (void)id;
    if (info) { info->facing = 0; info->orientation = 0; }
}

/* returns sp<CameraHardwareInterface>: the caller passes the place for it */
EXPORT void HAL_openCameraHardware(void **result, int id) {
    (void)id;
    *result = 0;
}
