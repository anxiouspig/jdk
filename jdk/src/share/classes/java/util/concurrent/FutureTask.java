/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * ��ȡ�����첽���㡣������ṩ��һ��Future�Ļ���ʵ�֣�ʹ��һЩ������������ȡ�����㡢
 * ��ѯ�Բ鿴�����Ƿ�����Լ�������������ֻ���ڼ�����ɺ󣬲ſɼ������;���������δ��ɣ�get������������
 * һ��������ɣ��Ͳ�������������ȡ������(����ʹ��runAndReset()���ü���)��
 *
 * <p>FutureTask����������װһ���ɵ��û�����еĶ�����ΪFutureTaskʵ����Runnable��
 * ���Կ��Խ�FutureTask�ύ��ִ�г���ִ��
 *
 * <p>������Ϊһ����������֮�⣬����໹�ṩ���ڴ����Զ���������ʱ�������õ��ܱ������ܡ�
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {
    /*
     * �޶�˵��:�����������ǰ�İ汾��ͬ����ǰ�İ汾������AbstractQueuedSynchronizer����Ҫ��Ϊ�˱�����ȡ�������ڼ䱣���ж�״̬���û��������⡣��ǰ����е�ͬ������������ͨ��CAS���µġ�state���ֶ����������������Լ�һ���򵥵�Treiber��ջ������ȴ����̡߳�
     *
     * Style note: ������һ���������ƹ���ʹ��AtomicXFieldUpdaters�Ŀ���������ֱ��ʹ�ò���ȫ���ڲ����ԡ�
     */

    /**
     * �����������״̬��������µġ�����״̬����set��setException��cancel������ת��Ϊ�ն�״̬��������ڼ䣬״̬���Բ�����ֵ̬�����(�����ý��ʱ)���ж�(�����ж����г���������ȡ��(true)ʱ)������Щ�м�״̬������״̬��ת��ʹ�ø����˵�����/�ӳ�д��������Ϊֵ��Ωһ�ģ����ܽ�һ���޸ġ�
     *
     * ���ܵ�״̬ת��:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    private volatile int state;
    private static final int NEW          = 0;
    private static final int COMPLETING   = 1;
    private static final int NORMAL       = 2;
    private static final int EXCEPTIONAL  = 3;
    private static final int CANCELLED    = 4;
    private static final int INTERRUPTING = 5;
    private static final int INTERRUPTED  = 6;

    /** �ײ����; ���к�Ϊ�� */
    private Callable<V> callable;
    /** get()���صĽṹ���쳣 */
    private Object outcome; // non-volatile, protected by state reads/writes
    /** �߳����еĽ��; ����ʱcas���� */
    private volatile Thread runner;
    /** Treiber stack of waiting threads */
    private volatile WaitNode waiters;

    /**
     * ���ؽ�����׳���������쳣��
     *
     * @param s ���״ֵ̬
     */
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        Object x = outcome; // ���
        if (s == NORMAL) // ��״̬Ϊ�������򷵻ؽ��
            return (V)x;
        if (s >= CANCELLED) // ��Ϊȡ�����жϣ����׳��쳣
            throw new CancellationException();
        throw new ExecutionException((Throwable)x); // ����״̬�׳��쳣
    }

    /**
     * ����һ�� {@code FutureTask} that will, ������, ִ�и����� {@code Callable}.
     *
     * @param  callable �ɵ��õ�����
     * @throws NullPointerException if the callable is null
     */
    public FutureTask(Callable<V> callable) {
        if (callable == null) // ������Ϊnull�����׳���ָ���쳣
            throw new NullPointerException();
        this.callable = callable; // ��ֵ����
        this.state = NEW;       // ȷ�����õĿɼ���
    }

    /**
     * ����һ�� {@code FutureTask} that will, ������ʱ, ִ��
     * ������ {@code Runnable}, ���� that {@code get} ȥ����
     * �ɹ���ɸ����Ľ����
     *
     * @param runnable ��������
     * @param result �ɹ���ɷ��صĽ��
     * �������Ҫ�ض��Ľ��, �������¹��캯��
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    public FutureTask(Runnable runnable, V result) {
        this.callable = Executors.callable(runnable, result); //
        this.state = NEW;       // ensure visibility of callable
    }
    // ������������������֮ǰ��ȡ�����򷵻�true��
    public boolean isCancelled() {
        return state >= CANCELLED;
    }
    // �������������ɣ��򷵻�true����ɿ�����������������ֹ���쳣��ȡ��������������Щ����£��˷�����������true��
    public boolean isDone() {
        return state != NEW;
    }

    // ��ͼȡ���������ִ�С���������Ѿ���ɣ��Ѿ�ȡ����������������ԭ���޷�ȡ������˳��Խ�ʧ�ܡ�����ɹ��������ڵ���cancelʱ��������δ�������������Ӧ���С���������Ѿ���������ômayInterruptIfRunning����ȷ��ִ�д�������߳��Ƿ�Ӧ���жϣ�����ͼֹͣ������
    // �ڴ˷������غ󣬶�Future.isDone()�ĺ������ý�ʼ�շ���true������÷�������true����ô��future.iscancel()�ĺ������ý�ʼ�շ���true��
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!(state == NEW && // ����߳�Ϊ�մ�����
              UNSAFE.compareAndSwapInt(this, stateOffset, NEW,
                  mayInterruptIfRunning ? INTERRUPTING : CANCELLED))) // �����߳�״̬Ϊ�жϻ�ȡ��
            return false; // ���ò��ɹ�������false
        try {    // �Է����ô�����쳣
            if (mayInterruptIfRunning) { // Ϊtrue���ж��������е��߳�
                try {
                    Thread t = runner; // �������е��߳�
                    if (t != null) // ����Ϊnull�����ж�
                        t.interrupt();
                } finally { // ״̬����Ϊ�ж�
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            finishCompletion();
        }
        return true;
    }

    /**
     * �����Ҫ����ȴ�������ɣ�Ȼ�����������
     * @throws CancellationException {@inheritDoc}
     */
    public V get() throws InterruptedException, ExecutionException {
        int s = state; // ״̬
        if (s <= COMPLETING)
            s = awaitDone(false, 0L); // �����ȴ��������
        return report(s);
    }

    /**
     * �����Ҫ�����ȴ���������ʱ������ɼ��㣬Ȼ���������(�������)��
     * @throws CancellationException {@inheritDoc}
     */
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();
        int s = state;
        if (s <= COMPLETING &&
            (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING) //  �����ȴ��������
            throw new TimeoutException();
        return report(s);
    }

    /**
     * �ڴ�����ת����״̬isDoneʱ�����ܱ����ķ���(��������������»���ͨ��ȡ��)��Ĭ��ʵ��ʲôҲ������������Ը��Ǵ˷�����������ɻص���ִ�в��ǡ�
     * ��ע�⣬�����Բ�ѯ�˷���ʵ���е�״̬����ȷ���������Ƿ��ѱ�ȡ����
     */
    protected void done() { }

    /**
     * ����future�Ľ������Ϊ����ֵ�����Ǵ�future�ѱ����û��ѱ�ȡ����
     *
     * <p>�˷����ڳɹ���ɼ������run()�������ڲ����á�
     *
     * @param v the value
     */
    protected void set(V v) { // ���ý��
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // ״̬��Ϊ�������
            outcome = v; // ��ֵ���
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state ״̬��Ϊ�������
            finishCompletion();
        }
    }

    /**
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    protected void setException(Throwable t) { // �����쳣��Ϣ
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) { // ״̬��Ϊ�������
            outcome = t; // ���Ϊ�쳣��Ϣ
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state ״̬��Ϊ�쳣״̬
            finishCompletion();
        }
    }

    public void run() {
        if (state != NEW || // �˷���Ҫ��״̬������new
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // �������̻߳�Ϊ��ǰ�߳�
            return; // �������������������أ�
        try {
            Callable<V> c = callable; // �ص�����
            if (c != null && state == NEW) { // �жϺ�����Ϊnull����״̬Ϊnew
                V result; // ���
                boolean ran; // �Ƿ�ɹ�
                try {
                    result = c.call();  // ���صĽ������һ����ʱ������ִ��ʱ��
                    ran = true; // ����ִ�гɹ�
                } catch (Throwable ex) {
                    result = null; // ���Ϊnull
                    ran = false; // ����ʧ��
                    setException(ex); // �����쳣�������쳣��Ϣ
                }
                if (ran)
                    set(result); // ���ý��
            }
        } finally {
            // runner �����null��until state is settled to
            // prevent concurrent calls to run()
            runner = null; // ִ�н�����ִ���߳���Ϊnull
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            int s = state;
            if (s >= INTERRUPTING) // ��������ж�
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset
     */
    protected boolean runAndReset() {
        if (state != NEW || // ״̬����Ϊnew
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread())) // ���������߳�Ϊ��ǰ�߳�
            return false;
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable; // �ص�����
            if (c != null && s == NEW) { // ������Ϊnull��״̬Ϊnew
                try {
                    c.call(); // �����ý��
                    ran = true;
                } catch (Throwable ex) {
                    setException(ex); // �����쳣��Ϣ
                }
            }
        } finally {
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            runner = null;
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            s = state;
            if (s >= INTERRUPTING) // ��Ϊ�ж�״̬
                handlePossibleCancellationInterrupt(s);
        }
        return ran && s == NEW;
    }

    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    private void handlePossibleCancellationInterrupt(int s) {
        // ���л���������֮ǰ�����ǵ��ж������ܻ�ֹͣ�����������ĵ�ѭ���ȴ���
        if (s == INTERRUPTING) // ��Ϊ�ж�״̬
            while (state == INTERRUPTING) // ѭ����ֻҪ�ж�״̬���ó�cpu
                Thread.yield(); // �ȴ��ж�

        // ���� state == INTERRUPTED;

        // ������Ҫȷ�������յ����κ��ж�cancel(true).
        //   Ȼ��, ����ʹ���ж�
        // ��Ϊһ�������Ļ��� ���ڹ�ͨ�����ߵ�����
        // and û�취ֻ���ȡ���ж�
        //
        // Thread.interrupted();

    }

    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    static final class WaitNode {
        volatile Thread thread; // �ȴ��߳�
        volatile WaitNode next; // ��һ���ȴ��߳�
        WaitNode() { thread = Thread.currentThread(); } // ��ǰ�߳���Ϊ�ȴ��߳�
    }

    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    private void finishCompletion() {
        // assert state > COMPLETING; // ״̬�����
        for (WaitNode q; (q = waiters) != null;) { // �ȴ��̲߳�Ϊnull
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) { // �ȴ��߳���Ϊnull
                for (;;) { // �����ͷŵȴ��߳�
                    Thread t = q.thread; // �ȴ��߳�
                    if (t != null) { // �̲߳�Ϊnull
                        q.thread = null; // GC
                        LockSupport.unpark(t); // �ͷŵȴ��߳�
                    }
                    WaitNode next = q.next; // ��һ���ȴ��߳�
                    if (next == null)
                        break;
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
        }

        done();

        callable = null;        // to reduce footprint
    }

    /**
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits
     * @param nanos time to wait, if timed
     * @return state upon completion
     */
    private int awaitDone(boolean timed, long nanos)
            throws InterruptedException {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;
        for (;;) { // �����ȴ��������
            if (Thread.interrupted()) { // ���߳��ж�
                removeWaiter(q); // �Ƴ��ȴ��߳�
                throw new InterruptedException();
            }

            int s = state; // ״̬
            if (s > COMPLETING) { // �����
                if (q != null)
                    q.thread = null;
                return s; // ����״̬
            }
            else if (s == COMPLETING) // cannot time out yet
                Thread.yield(); // δ������ó�cpu
            else if (q == null)
                q = new WaitNode();
            else if (!queued)
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset,
                        q.next = waiters, q); // �滻�ȴ��߳�
            else if (timed) { // ��ʱ
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    removeWaiter(q); // �Ƴ��ȴ��߳�
                    return state;
                }
                LockSupport.parkNanos(this, nanos);
            }
            else
                LockSupport.park(this); // ������ǰ�߳�
        }
    }

    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    private void removeWaiter(WaitNode node) {
        if (node != null) {
            node.thread = null;
            retry:
            for (;;) {          // �����ҵ��ýڵ㣬�Ƴ�
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    s = q.next;
                    if (q.thread != null)
                        pred = q;
                    else if (pred != null) {
                        pred.next = s;
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset,
                            q, s))
                        continue retry;
                }
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;
    private static final long runnerOffset;
    private static final long waitersOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
