package com.example.commandaliases;

/** One step of a script: either a command to send, or a pause (a "wait"). */
public record Action(String command, int waitTicks) {
    public static Action cmd(String command) {
        return new Action(command, 0);
    }

    public static Action wait(int ticks) {
        return new Action(null, Math.max(0, Math.min(1200, ticks)));
    }

    public boolean isWait() {
        return command == null;
    }
}
