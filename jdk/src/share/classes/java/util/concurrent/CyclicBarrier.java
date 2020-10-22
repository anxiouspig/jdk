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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * һ��ͬ���������ߣ�����һ���̱߳˴˵ȴ�����һ����ͬ���ϰ��㡣
 * cyclicbarrier�ڰ����̶���С���̵߳ĳ����зǳ����ã���Щ�߳���ʱ����˴˵ȴ���
 * ������ϱ���Ϊѭ�����ϣ���Ϊ�������ڵȴ����̱߳��ͷź�����ʹ�á�
 *
 * <p>CyclicBarrier֧��һ����ѡ��Runnable�����������ÿһ�����ϵ�������һ�Σ�
 * �����һ���̵߳���֮�󣬵������ͷ��κ��߳�֮ǰ��������϶����������κ�һ������֮ǰ���¹���״̬�ǳ����á�
 *
 * <p>������һ���ڲ��зֽ������ʹ�����ϵ�����:
 *
 *  <pre> {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}</pre>
 *
 * �����ÿ�������̴߳�������һ�У�Ȼ�������ϴ��ȴ���ֱ�������������С�
 * �ڴ���������ʱ��ִ�����ṩ��Runnable barrier�������ϲ��С�����ϲ�ȷ���ҵ��˽��������
 * ��ôdone()������true��ÿ��worker����ֹ��
 *
 * <p>���barrier������ִ��ʱ�������ڱ�����Ĳ��뷽��
 * ��ô���뷽�е��κ��̶߳��������ͷŸò���ʱִ�иò�����
 * Ϊ�˴ٽ���һ�㣬ÿ�ε���await()���᷵�ظ��߳������ϴ��ĵ���������
 * Ȼ�������ѡ���ĸ��߳�Ӧ��ִ�������ж�������:
 *  <pre> {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}</pre>
 *
 * <p>CyclicBarrierʹ��һ���������޵�����ģ��ͬ������ʧ��:���һ���߳��뿪�ϰ�������ж�,ʧ��,��ʱ,
 * ���������̵߳ȴ��ϰ���Ҳ���뿪�쳣ͨ��BrokenBarrierException
 * (��InterruptedException�������Ҳ������˴�Լ��ͬһʱ��)��
 *
 * <p>�ڴ�һ����ЧӦ:�ڵ���await()֮ǰ���߳��еĲ�������Щ������barrier������һ���֣���barrier���������������߳�����Ӧ��await()�ɹ�����֮���before������
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions that are part of the barrier action, which in turn
 * <i>happen-before</i> actions following a successful return from the
 * corresponding {@code await()} in other threads.
 *
 * @since 1.5
 * @see CountDownLatch
 *
 * @author Doug Lea
 */
public class CyclicBarrier {
    /**
     * ���ϵ�ÿ��ʹ�ö���ʾΪһ������ʵ�������ۺ�ʱ�������ϻ��������ϣ�����������ɡ��������������߳���ص�ʹ���ϰ�,���ڲ�ȷ���ķ�ʽ�����ܱ����䵽�ȴ��߳�,��һ��ֻ�ܼ���һ��(һ��{@code��}����)������������������������Ѿ��жϵ�û�к������ã�����Ҫ����ɡ�
     */
    private static class Generation {
        boolean broken = false;
    }

    /** �������� */
    private final ReentrantLock lock = new ReentrantLock();
    /** �������ȴ� */
    private final Condition trip = lock.newCondition();
    /** The number of parties */
    private final int parties;
    /* The command to run when tripped */
    private final Runnable barrierCommand;
    /** The current generation */
    private Generation generation = new Generation();

    /**
     * ��������ڵȴ���ÿ���Ӿۻ�����0���������õ�ÿһ����һ���������򵱴��ơ�
     */
    private int count;

    /**
     * ����״̬�����������߳�
     */
    private void nextGeneration() {
        // signal completion of last generation
        trip.signalAll();
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    /**
     * ����ǰ��������������Ϊ�����ƣ������������ˡ�ֻ���ڳ�����ʱ�ŵ��á�
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    /**
     * ��Ҫ���ϴ��룬���Ǹ������ߡ�
     */
    private int dowait(boolean timed, long nanos)
        throws InterruptedException, BrokenBarrierException,
               TimeoutException {
        // �õ���������
        final ReentrantLock lock = this.lock;
        // ����
        lock.lock();
        try {
            final Generation g = generation;

            if (g.broken) // �������ˣ����쳣
                throw new BrokenBarrierException();

            if (Thread.interrupted()) { // ���ж�
                breakBarrier();
                throw new InterruptedException();
            }

            int index = --count; // ����-1
            if (index == 0) {  // tripped
                boolean ranAction = false;
                try {
                    final Runnable command = barrierCommand; // run����
                    if (command != null)
                        command.run(); // ��Ϊnull�������к���
                    ranAction = true;
                    nextGeneration(); // �����߳�
                    return 0;
                } finally {
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // ѭ�� until tripped, broken, ���, or ��ʱ
            for (;;) {
                try {
                    if (!timed) // ��ʱ��ģʽ
                        trip.await(); // ������
                    else if (nanos > 0L) // �г�ʱʱ�������
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    if (g == generation && ! g.broken) { // �жϵĻ�
                        breakBarrier();
                        throw ie;
                    } else {
                        // ��ʹû�б��жϣ�����Ҳ����ɵȴ����������жϱ���Ϊ�����ڡ�����ִ�С�
                        Thread.currentThread().interrupt();
                    }
                }

                if (g.broken)
                    throw new BrokenBarrierException();

                if (g != generation)
                    return index;

                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * ����һ���µ�CyclicBarrier�������������Ĳ��뷽(�߳�)���ڵȴ���ʱ��������բ����������բʱ��
     * ����ִ�и�����barrier�����������һ���������ϵ��߳�ִ�С�
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;// �ڴ�������֮ǰ�������await()���̵߳�����
        this.count = parties;
        this.barrierCommand = barrierAction; // ִ�к���
    }

    /**
     * ����һ���µ�CyclicBarrier�������������Ĳ��뷽(�߳�)���ڵȴ���ʱ��
     * ��������բ����������բʱ����ִ��Ԥ����Ĳ�����
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    /**
     * ������Ҫ��Խ�����ϵĲ��뷽��������
     *
     * @return the number of parties required to trip this barrier
     */
    public int getParties() {
        return parties;
    }

    /**
     * �ȴ���ֱ�����в��뷽���ѵ��ô������ϵĵȴ���
     *
     * <p>�����ǰ�̲߳�����󵽴�ģ���ô�����̵߳��ȵ�Ŀ�ģ����������ã�����������״̬��ֱ�������������֮һ:
     * <ul>
     * <li>���һ���̵߳���;
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     * <li>����һЩ�̻߳��ж�һ�����ڵȴ����߳�;
     * <li>����һЩ�߳��ڵȴ�barrierʱ��ʱ;
     * <li>����һЩ�߳�����������ϵ���reset()��
     * </ul>
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڵȴ�ʱ�ж�,Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * ������κ��̵߳ȴ�ʱ���ݱ�����()�������ڵ���awaitʱ���ݱ����ƣ��������κ��̵߳ȴ�ʱ���ݱ����ƣ�
     * ��ô�׳�broken barrierexception��
     *
     * <p>����κ��߳��ڵȴ�ʱ���жϣ���ô���������ȴ��߳̽��׳�broken barrierexception��
     * ��barrier������broken״̬��
     *
     * <p>�����ǰ�߳������һ��������̣߳����ҹ��캯�����ṩ��һ���ǿ����ϲ�����
     * ��ô��ǰ�߳������������̼߳���֮ǰ���иò�����
     * ���barrier�����ڼ䷢���쳣����ô���쳣���ڵ�ǰ�߳��д�����barrier�������ж�״̬��
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     */
    public int await(long timeout, TimeUnit unit)
        throws InterruptedException,
               BrokenBarrierException,
               TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    /**
     * ��ѯ�������Ƿ����ж�״̬��
     *
     * @return {@code true} if one or more parties broke out of this
     *         barrier due to interruption or timeout since
     *         construction or the last reset, or a barrier action
     *         failed due to an exception; {@code false} otherwise.
     */
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * ����������Ϊ���ʼ״̬������κ�һ����ǰ�������ϴ��ȴ������ǽ�����һ������������쳣��
     * ��ע�⣬��������ԭ����ɵ�����������и�λ���ܱȽϸ���;
     * �߳���Ҫ��������ʽ����ͬ������ѡ��һ�ַ�ʽִ�����á������Ϊ����ʹ�ô���һ���µ����ϡ�
     */
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            breakBarrier();   // break the current generation
            nextGeneration(); // start a new generation
        } finally {
            lock.unlock();
        }
    }

    /**
     * ���ص�ǰ�����ϴ��ȴ��Ĳ��뷽���������˷�����Ҫ���ڵ��ԺͶ��ԡ�
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
