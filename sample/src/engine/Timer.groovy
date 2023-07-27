package engine

import groovy.transform.CompileStatic

@CompileStatic
class Timer {

    private long next = 0
    private long interval
    private Listener listener

    boolean isCancel = false

    Timer(long next, long interval, Listener listener) {
        this.next = next
        this.interval = interval
        this.listener = listener
    }

    void cancel() {
        isCancel = true
    }

    void run(long time) {
        if (time >= next) {
            next = next + interval
            listener.trigger(this)
        }
    }

    @CompileStatic
    static interface Listener {
        void trigger(Timer timer)
    }
}
