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
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * һ��ͬ������������һ�������̵߳ȴ���ֱ���������߳���ִ�е�һ�������ɡ�
 *
 * <p>CountDownLatch���ø�����count��ʼ���ġ����ڵ�����countDown()������await����������
 * ֱ����ǰ����Ϊ�㣬֮���ͷ����еȴ��̣߳������������κκ�����await���á�
 * ����һ��һ�������󡪡������޷����á������Ҫ���ü����İ汾�����Կ���ʹ��CyclicBarrier��
 *
 * <p>CountDownLatch��һ��ͨ�õ�ͬ�����ߣ��������ڶ�����;��
 * countΪ1ʱ��ʼ����CountDownLatch�����򵥵�on/off latch��gate:���е���wait���̶߳���gate���ȴ���
 * ֱ������countDown()���̴߳�����һ����ʼ��ΪN��CountDownLatch����������һ���̵߳ȴ���
 * ֱ��N���߳����ĳ������������ĳ�������Ѿ����N�Ρ�
 *
 * <p>ACountDownLatch��һ�����õ������ǣ�������Ҫ���õ���ʱ���̵߳ȴ������ﵽ��ż�����
 * ��ֻ�Ƿ�ֹ�κ��̼߳����ȴ���ֱ�������̶߳�ͨ����
 *
 * <p><b>�����������࣬����һ�鹤���߳�ʹ����������ʱ������:
 * <ul>
 * <li>��һ���������źţ�����ֹ�κ�worker����������ֱ����������׼���������Ǽ�������;
 * <li>�ڶ���������źţ�����˾���ȴ���ֱ�����й�����ɡ�
 * </ul>
 *
 *  <pre> {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>��һ�ֵ��͵��÷��ǽ�һ������ֳ�N�����֣���һ��Runnable������ÿ�����֣�
 * ��Runnableִ�иò��ֲ����������ϼ�����Ȼ������Runnables�ŶӸ�һ��ִ������
 * �������Ӳ�����ɺ�Э���߳̽��ܹ�ͨ��wait��(���̱߳��������ַ�ʽ�ظ�����ʱ��ʹ��CyclicBarrier��)
 *
 *  <pre> {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>�ڴ�һ����ЧӦ:�ڼ���Ϊ0֮ǰ���ڵ���countDown()֮ǰ���߳��еĲ����������ڴ���һ��
 * �߳�����Ӧ��await()�ɹ�����֮ǰ�Ĳ���������
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {
    /**
     * ����ʱ��������ͬ�����ơ�ʹ��AQS״̬��ʾ������
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) { // ����״̬��
            setState(count);
        }

        int getCount() { // �õ�������
            return getState();
        }

        protected int tryAcquireShared(int acquires) { // ��ù����� == await
            return (getState() == 0) ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) { // �ͷŹ�����
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * ����һ�� {@code CountDownLatch} ��ʼ������������
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * ���µ�ǰ�̵߳ȴ���ֱ���������������㣬�����̱߳��жϡ�
     *
     * <p>�����ǰ����Ϊ�㣬��˷����������ء�
     *
     * <p>�����ǰ�߳�������0����ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ����ã�����������״̬��ֱ�����������������֮һ:
     * <ul>
     * <li>���ڵ�����countDown()����������Ϊ��;
     * <li>����һЩ�߳��жϵ�ǰ�̡߳�
     * </ul>
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڵȴ�ʱ�жϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1); // ��ȡ�����ж���
    }

    /**
     * ���µ�ǰ�̵߳ȴ���ֱ���������������㣬�����̱߳��жϣ�����ָ���ĵȴ�ʱ���ѹ���
     *
     * <p>�����ǰ����Ϊ�㣬��˷�����������ֵtrue��
     *
     * <p>�����ǰ�߳�������0����ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ����ã�����������״̬��ֱ�����������������֮һ:
     * <ul>
     * <li>���ڵ�����countDown()����������Ϊ��;
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     * </ul>
     *
     * <p>�������Ϊ�㣬��÷�������ֵtrue��
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڵȴ�ʱ�жϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>���ָ���ĵȴ�ʱ����ڣ��򷵻�falseֵ�����ʱ��С�ڻ����0����÷�����������ȴ���
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * �ݼ��������ļ������������Ϊ�㣬���ͷ����еȴ����̡߳�
     *
     * <p>�����ǰ���������㣬��ݼ�������¼���Ϊ�㣬��ô���еȴ����̶߳����������ã��Ա�����̵߳��ȡ�
     *
     * <p>�����ǰ��������0����ʲôҲ���ᷢ����
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * ���ص�ǰ������
     *
     * <p>�˷���ͨ�����ڵ��ԺͲ���Ŀ�ġ�
     *
     * @return the current count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * ���ر�ʶ�����������ַ�������״̬���������е�״̬�����ַ�����Count =���͵�ǰ������
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
