#include <vector>
#include <sstream>
#include <ff_api.h>
#include <sys/un.h>
#include <arpa/inet.h>
#include <asm-generic/ioctls.h>
#include "string"

#include "algorithm"

#define PATH_MAX 4096
#define MAX_EVENTS 512

typedef int (*loop_func)(signed char *arg);

namespace ff {
    int backlog = 1024;

    struct kevent kevSet;
    struct kevent events[MAX_EVENTS];

    int init(const std::string &args) {
        printf("init args: %s\n", args.c_str());

        std::stringstream ss(args);
        std::string item;
        std::vector<std::string> elems;
        while (getline(ss, item, ' ')) {
            if (!item.empty()) {
                elems.push_back(item);
            }
        }

        int argc = elems.size();
        char **argv = static_cast<char **>(malloc(sizeof(char *) * argc));
        int i;
        for (i = 0; i < argc; i++) {
            argv[i] = static_cast<char *>(malloc(sizeof(char) * PATH_MAX));
            sprintf(argv[i], elems.at(i).c_str());
        }
        elems.clear();

        int r = ff_init(argc, argv);
        printf("ff init result: %d\n", r);

        int r2 = ff_kqueue();
        if (r2 < 0) {
            printf("ff_kqueue failed, errno:%d, %s\n", errno, strerror(errno));
        }
        return r2;
    }

    int socket() {
        int sock_fd = ff_socket(AF_INET, SOCK_STREAM, 0);
        if (sock_fd < 0) {
            printf("ff_socket failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return sock_fd;
    }

    int ioctl_nio(int sock_fd) {
        int on = 1;
        int r = ff_ioctl(sock_fd, FIONBIO, &on);
        if (r < 0) {
            printf("ff_ioctl failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return r;
    }

    int connect(int sock_fd, std::string address, int port) {
        struct sockaddr_in addr;
        bzero(&addr, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = inet_addr(address.c_str());
        addr.sin_port = htons(port);

        int r = ff_connect(sock_fd, (struct linux_sockaddr *) &addr, sizeof(addr));
        if (r < 0) {
            printf("ff_connect failed, addr:%s, sock_fd:%d, errno:%d, %s\n", address.c_str(), sock_fd,
                   errno, strerror(errno));
        }
        return r;
    }

    int bind(int sock_fd, int port) {
        struct sockaddr_in addr;
        bzero(&addr, sizeof(addr));
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = htonl(INADDR_ANY);
        addr.sin_port = htons(port);

        int r = ff_bind(sock_fd, (struct linux_sockaddr *) &addr, sizeof(addr));
        if (r < 0) {
            printf("ff_bind failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return r;
    }

    int listen(int sock_fd) {
        int r = ff_listen(sock_fd, backlog);
        if (r < 0) {
            printf("ff_listen failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return r;
    }

    int close(int sock_fd) {
        int r = ff_close(sock_fd);
        if (r < 0) {
            printf("ff_close failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return r;
    }

    int accept(int sock_fd) {
        int r = ff_accept(sock_fd, NULL, NULL);
        if (r < 0) {
            printf("ff_accept failed, sock_fd:%d, errno:%d, %s\n", sock_fd, errno, strerror(errno));
        }
        return r;
    }

    int write(int sock_fd, void *buf, int len) {
        return ff_write(sock_fd, buf, len);
    }

    int read(int sock_fd, void *buf, int len) {
        return ff_read(sock_fd, buf, len);
    }

    int kevent(int handler_id, bool with_ev, int timeout) {
        return ff_kevent(handler_id, with_ev ? &kevSet : NULL, with_ev ? 1 : 0,
                         with_ev ? NULL : events, with_ev ? 0 : MAX_EVENTS, NULL);
    }

    void evt_set(int sock_fd, short event_filter, short flags) {
        EV_SET(&kevSet, sock_fd, event_filter, flags, 0, 0, NULL);
    }

    void evt_set_server_sock(int sock_fd, short event_filter, short flags) {
        EV_SET(&kevSet, sock_fd, event_filter, flags, 0, MAX_EVENTS, NULL);
    }

    int evt_fd(int index) {
        struct kevent event = events[index];
        return (int) event.ident;
    }

    int evt_flags(int index) {
        struct kevent event = events[index];
        return event.flags;
    }

    int evt_filter(int index) {
        struct kevent event = events[index];
        return event.filter;
    }

    int evt_data(int index) {
        struct kevent event = events[index];
        return event.data;
    }

    void run(loop_func loop) {
        ff_run((loop_func_t) loop, NULL);
    }

    short ev_eof() {
        return EV_EOF;
    }

    short ev_add() {
        return EV_ADD;
    }

    short ev_delete() {
        return EV_DELETE;
    }

    short evfilt_read() {
        return EVFILT_READ;
    }

    short evfilt_write() {
        return EVFILT_WRITE;
    }
}
