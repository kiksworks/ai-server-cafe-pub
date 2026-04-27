package ai_server_cafe.controller;

import java.util.ArrayDeque;
import java.util.Deque;

public abstract class PID<T> {
    protected final double kP;
    protected final double kI;
    protected final double kD;
    protected final double dt;
    protected T prevError;
    protected T integral;
    protected final Deque<T> que;
    protected final int bufferSize;

    public PID(double kP, double kI, double kD, double dt, T initIntegral, T initPrevError, int bufferSize) {
        this.integral = initIntegral;
        this.prevError = initPrevError;
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.dt = dt;
        this.que = new ArrayDeque<>();
        this.bufferSize = bufferSize;
    }

    public abstract T update(T error);
}
