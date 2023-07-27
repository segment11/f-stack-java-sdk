package ff;

import org.bytedeco.javacpp.FunctionPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.ByPtr;
import org.bytedeco.javacpp.annotation.Namespace;
import org.bytedeco.javacpp.annotation.Platform;

@Platform(include = "library.hpp")
@Namespace("ff")
public class Invoker {
    static {
        Loader.load();
    }

    public static class Callback extends FunctionPointer {
        static {
            Loader.load();
        }

        protected Callback() {
            allocate();
        }

        private native void allocate();

        public int call(java.nio.ByteBuffer args) {
            return -1;
        }
    }

    public static native void backlog(int backlog);

    public static native int backlog();

    public static native int init(String argv);

    public static native int socket();

    public static native int connect(int sock_fd, String addr, int port);

    public static native int bind(int sock_fd, int port);

    public static native int listen(int sock_fd);

    public static native int close(int sock_fd);

    public static native int accept(int sock_fd);

    public static native int write(int sock_fd, java.nio.Buffer buffer, int len);

    public static native int read(int sock_fd, java.nio.Buffer buffer, int len);

    public static native int ioctl_nio(int sock_fd);

    public static native int kevent(int handler_id, boolean with_ev, int timeout);

    public static native void evt_set(int sock_fd, short event_filter, short flags);

    public static native void evt_set_server_sock(int sock_fd, short event_filter, short flags);

    public static native int evt_fd(int index);

    public static native short evt_flags(int index);

    public static native short evt_filter(int index);

    public static native int evt_data(int index);

    public static native void run(@ByPtr Callback runner);

    public static native short ev_eof();

    public static native short ev_add();

    public static native short ev_delete();

    public static native short evfilt_read();

    public static native short evfilt_write();
}
