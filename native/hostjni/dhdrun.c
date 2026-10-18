// Open-source replacement for dhdrun (Issue #15)
// Usage: dhdrun <props_path> <props_fd> <command> [args...]

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/un.h>

extern char **environ;

int main(int argc, char **argv) {
    if (argc < 4) {
        fprintf(stderr, "Usage: %s <props_path> <props_fd> <command> [args...]\n", argv[0]);
        return 1;
    }

    const char *props_path = argv[1];
    int target_fd = atoi(argv[2]);
    const char *cmd = argv[3];

    // Open property workspace
    int pfd = open(props_path, O_RDWR);
    if (pfd < 0) {
        pfd = open(props_path, O_RDONLY);
    }
    if (pfd >= 0) {
        struct stat st;
        if (fstat(pfd, &st) == 0) {
            if (pfd != target_fd) {
                if (dup2(pfd, target_fd) >= 0) {
                    close(pfd);
                    pfd = target_fd;
                }
            }
            // Clear FD_CLOEXEC so guest inherits it
            int flags = fcntl(target_fd, F_GETFD);
            if (flags >= 0) {
                fcntl(target_fd, F_SETFD, flags & ~FD_CLOEXEC);
            }
            char buf[64];
            snprintf(buf, sizeof(buf), "%d,%lld", target_fd, (long long)st.st_size);
            setenv("ANDROID_PROPERTY_WORKSPACE", buf, 1);
        } else {
            close(pfd);
        }
    }

    // Process DHD_SOCK_ and DHD_ENV_ from environment
    // Collect keys to modify/remove to avoid modifying environ while iterating
    char **env_copy = NULL;
    int env_count = 0;
    while (environ && environ[env_count]) env_count++;

    for (int i = 0; i < env_count; i++) {
        char *entry = environ[i];
        if (!entry) continue;

        if (strncmp(entry, "DHD_SOCK_", 9) == 0) {
            // Format: DHD_SOCK_<name>=<path>,<octal_mode>
            char *eq = strchr(entry + 9, '=');
            if (!eq) continue;

            int name_len = (int)(eq - (entry + 9));
            char sock_name[64];
            if (name_len >= (int)sizeof(sock_name)) name_len = sizeof(sock_name) - 1;
            memcpy(sock_name, entry + 9, name_len);
            sock_name[name_len] = '\0';

            char *val = eq + 1;
            char sock_path[108];
            char *comma = strchr(val, ',');
            int path_len = comma ? (int)(comma - val) : (int)strlen(val);
            if (path_len >= (int)sizeof(sock_path)) path_len = sizeof(sock_path) - 1;
            memcpy(sock_path, val, path_len);
            sock_path[path_len] = '\0';

            mode_t mode = 0660;
            if (comma) {
                mode = (mode_t)strtol(comma + 1, NULL, 8);
                if (mode == 0) mode = 0660;
            }

            int sfd = socket(AF_UNIX, SOCK_STREAM, 0);
            if (sfd >= 0) {
                unlink(sock_path);
                struct sockaddr_un sa;
                memset(&sa, 0, sizeof(sa));
                sa.sun_family = AF_UNIX;
                strncpy(sa.sun_path, sock_path, sizeof(sa.sun_path) - 1);

                if (bind(sfd, (struct sockaddr *)&sa, sizeof(sa)) == 0) {
                    chmod(sock_path, mode);
                    listen(sfd, 4);

                    // Clear cloexec
                    int fl = fcntl(sfd, F_GETFD);
                    if (fl >= 0) fcntl(sfd, F_SETFD, fl & ~FD_CLOEXEC);

                    char env_var[128];
                    snprintf(env_var, sizeof(env_var), "ANDROID_SOCKET_%s", sock_name);
                    char fd_buf[16];
                    snprintf(fd_buf, sizeof(fd_buf), "%d", sfd);
                    setenv(env_var, fd_buf, 1);
                } else {
                    close(sfd);
                }
            }
        } else if (strncmp(entry, "DHD_ENV_", 8) == 0) {
            // Format: DHD_ENV_<key>=<value>
            char *eq = strchr(entry + 8, '=');
            if (eq) {
                int key_len = (int)(eq - (entry + 8));
                char key[128];
                if (key_len >= (int)sizeof(key)) key_len = sizeof(key) - 1;
                memcpy(key, entry + 8, key_len);
                key[key_len] = '\0';

                setenv(key, eq + 1, 1);
            }
        }
    }

    // Clean up DHD_* environment variables
    for (int i = 0; i < env_count; i++) {
        char *entry = environ[i];
        if (!entry) continue;
        if (strncmp(entry, "DHD_", 4) == 0) {
            char *eq = strchr(entry, '=');
            if (eq) {
                char key[128];
                int kl = (int)(eq - entry);
                if (kl >= (int)sizeof(key)) kl = sizeof(key) - 1;
                memcpy(key, entry, kl);
                key[kl] = '\0';
                unsetenv(key);
            }
        }
    }

    setpgid(0, 0);

    // Execute wrapped command
    execv(cmd, &argv[3]);
    perror("dhdrun: execv failed");
    return 127;
}
