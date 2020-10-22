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
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * �����ź������Ӹ����Ͻ����ź���ά��һ�����֤�������Ҫ��ÿ��acquire()����������
 * ֱ��������֤��Ȼ���ȡ���֤��ÿ��release()���һ�����֤��Ǳ�ڵ��ͷ�һ�������Ļ�ȡ�ߡ�
 * ���ǣ�û��ʵ��ʹ�����֤����;�ź���ֻ�Ǳ��ֿ���������һ������������Ӧ�ؽ��в�����
 *
 * <p>�ź���ͨ�����������ܹ�����ĳЩ(������߼�)��Դ���߳����������磬������һ����ʹ���ź��������ƶ���صķ���:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * <p>�ڻ��һ����֮ǰ��ÿ���̱߳�����ź������һ����ɣ���ȷ��һ�����ǿ��õġ����̴߳����������������ص����У�
 * �����ź�������һ�����֤���Ӷ�������һ���̻߳�ȡ�����ע�⣬�ڵ���acquire()ʱ�������ͬ������
 * ��Ϊ�����ֹ����ص����С��ź�����װ�����ƶԳصķ��������ͬ������ά���ر����һ����������κ�ͬ���ֿ���
 *
 * <p>һ����ʼ��Ϊһ�����ź���������ʹ�÷�ʽ�����ֻ��һ�����õ����֤��������������������ͨ������Ϊ�������ź�����
 * ��Ϊ��ֻ������״̬:һ���������֤������0���������֤���������ַ�ʽʹ��ʱ��
 * �������ź�����������������(�������ʵ�ֲ�ͬ)����������������������������߳��ͷ�(��Ϊ�ź���û������Ȩ�ĸ���)��
 * ����ĳЩ�ض��������к����ã����������ָ���
 *
 * <p> ����Ĺ��캯����ѡ�ؽ��ܹ�ƽ�Բ�����������Ϊfalseʱ�����಻�ܱ�֤�̻߳����ɵ�˳��
 * �ر��ǣ������ң�Ҳ����˵��������һֱ�ڵȴ����߳�֮ǰΪ����acquire()���̷߳������֤�������߼��Ͻ���
 * ���߳̽��Լ����ڵȴ��̶߳��е���ǰ�档����ƽ������Ϊ��ʱ���ź�����֤��ѡ������κλ�ȡ�������̣߳�
 * �Ӷ��������Ƕ���Щ�����ĵ��õĴ���˳��(�Ƚ��ȳ�;�Ƚ��ȳ�)��
 * ��ע�⣬FIFO˳���Ȼ��������Щ�����е��ض��ڲ�ִ�е㡣
 * ��ˣ�һ���߳̿�������һ���߳�֮ǰ����acquire����������һ���߳�֮�󵽴�����㣬
 * ͬ���أ��ڷ�������ʱ��������㡣����ע�⣬����ʱtryAcquire���������ع�ƽ���ã�������ȡ�κ�����ǿ��õġ�
 *
 * <p>ͨ�������ڿ�����Դ���ʵ��ź���Ӧ�ñ���ʼ��Ϊ��ƽ�ģ���ȷ��û���߳���Ϊ������Դ��������
 * �����ź��������������͵�ͬ������ʱ���ǹ�ƽ��������������Ƴ��������˹�ƽ�Կ��ǡ�
 *
 * <p>����໹�ṩ��һ�λ�ȡ���ͷŶ����ɵı�����������ʹ����Щ������������ƽ������Ϊ��ʱ��Ҫע���������ӳٵķ��ա�
 *
 * <p>�ڴ�һ����ЧӦ:�ڵ��á�release������(��release())֮ǰ���߳��еĲ�����������һ���߳��е�acquire()
 * �ȳɹ��ġ�acquire������֮��Ĳ�������֮ǰ��
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {
    private static final long serialVersionUID = -3222578661600680210L;
    /** ͨ��apsʵ�ֵ��� */
    private final Sync sync;

    /**
     * ͬ��ʵ�� for semaphore.  �� AQS state
     * �������. ����ʵ�ֹ�ƽ�ͷǹ�ƽ�汾
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1192457210091910933L;
        // ����state
        Sync(int permits) {
            setState(permits);
        }
        // �õ�state
        final int getPermits() {
            return getState();
        }
        // �ǹ�ƽ��ù�����
        final int nonfairTryAcquireShared(int acquires) {
            for (;;) { // ����
                int available = getState(); // �õ����
                int remaining = available - acquires; // ʣ�����
                if (remaining < 0 || // û����ɻ���������ɳɹ�
                    compareAndSetState(available, remaining))
                    return remaining; // �������
            }
        }
        // �ͷŹ�����
        protected final boolean tryReleaseShared(int releases) {
            for (;;) { // ����
                int current = getState(); // �õ����
                int next = current + releases;// ����
                if (next < current) // overflow // �ͷ���С���ͷ�ǰ�����쳣
                    throw new Error("Maximum permit count exceeded");
                if (compareAndSetState(current, next)) // �滻״̬
                    return true;
            }
        }
        // �����
        final void reducePermits(int reductions) {
            for (;;) { // ����
                int current = getState(); // �õ����
                int next = current - reductions; // ����
                if (next > current) // overflow // �ͷ�������ͷ�ǰ�����쳣
                    throw new Error("Permit count underflow");
                if (compareAndSetState(current, next)) // �滻״̬
                    return;
            }
        }
        // ��ɹ�0
        final int drainPermits() {
            for (;;) {
                int current = getState();
                if (current == 0 || compareAndSetState(current, 0))
                    return current;
            }
        }
    }

    /**
     * �ǹ�ƽ�汾
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = -2694183684443567898L;

        NonfairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    /**
     * ��ƽ�汾
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = 2014338818796000944L;

        FairSync(int permits) {
            super(permits);
        }

        protected int tryAcquireShared(int acquires) {
            for (;;) {
                if (hasQueuedPredecessors())
                    return -1;
                int available = getState();
                int remaining = available - acquires;
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;
            }
        }
    }

    /**
     * ����һ�� {@code Semaphore} �ø�����������ɺͷǹ�ƽ����
     *
     * @param permits ����ɻ�ó�ʼ�������
     *        ���ֵ�����Ǹ���, ���κ�������ɺ��ͷ�
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * ����һ�� {@code Semaphore} �ø�����������ɺ͹�ƽ����
     *
     * @param permits �������
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * ������ź������һ����ɣ�����ֱ����һ�����ã������̱߳��жϡ�
     *
     * <p>������֤����������֤���������أ����ɻ�õ����֤��������һ����
     *
     * <p>���û�п��õ����֤����ô��ǰ�߳̽������̵߳��ȵ�Ŀ�ı����ã�����������״̬��ֱ�����������������֮һ:
     * <ul>
     * <li>����һЩ�̵߳�������ź�����release()��������ǰ�߳̽�����������һ�����֤;
     * <li>����һЩ�߳��жϵ�ǰ�̡߳�
     * </ul>
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڵȴ����֤ʱ����ϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * ������ź��������ɣ�����ֱ����һ�����á�
     *
     * <p>������֤����������֤���������أ����ɻ�õ����֤��������һ����
     *
     * <p>���û�п��õ����֤����ô��ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�����������״̬��
     * ֱ�������̵߳��ô��ź�����release()������Ȼ��Ϊ��ǰ�̷߳������֤��
     *
     * <p>�����ǰ�߳��ڵȴ����֤�Ĺ����б��жϣ���ô���������ȴ���
     * ���Ƿ�����߳����֤��ʱ�������û���жϵ���������յ����֤��ʱ�䲻ͬ��
     * ���̴߳������������ʱ�������ж�״̬�������á�
     */
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * ���ڵ���ʱ���õ�����£��Ӹ��ź�����ȡ���֤��
     *
     * <p>��������֤���������֤���������أ�ֵΪtrue���Ӷ����������֤����������1��
     *
     * <p>���û�����֤���ã���ô�����������������ֵfalse��
     *
     * <p>��ʹ������ź�������Ϊʹ�ù�ƽ��������ԣ������һ�����õ����֤��
     * ��ô����tryAcquire()������������֤�����������߳��Ƿ����ڵȴ���
     * ���֡���ײ����Ϊ��ĳЩ����������õģ���ʹ���ƻ��˹�ƽ������������ع�ƽ���ã�
     * ��ôʹ��tryAcquire(0, TimeUnit.SECONDS)���⼸���ǵȼ۵�(��Ҳ����ж�)��
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * ����ڸ����ĵȴ�ʱ���ڿ��ò��ҵ�ǰ�߳�û�б��жϣ���Ӹ��ź��������ɡ�
     *
     * <p>��������֤���������֤���������أ�ֵΪtrue���Ӷ����������֤����������1��
     *
     * <p>���û�п��õ���ɣ���ô��ǰ�߳̽������̵߳��ȵ�Ŀ�ı����ã�����������״̬��ֱ�����������������֮һ:
     * <ul>
     * <li>����һЩ�̵߳�������ź�����release()��������ǰ�߳̽�����������һ�����֤;
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     * <li>ָ���ĵȴ�ʱ���Ѿ����ˡ�
     * </ul>
     *
     * <p>���������֤���򷵻�trueֵ��
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڵȺ����֤ʱ����ϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>���ָ���ĵȴ�ʱ����ڣ��򷵻�falseֵ�����ʱ��С�ڻ����0����÷�����������ȴ���
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    public boolean tryAcquire(long timeout, TimeUnit unit)
        throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * �ͷ����֤�����䷵�ص��ź�����
     *
     * <p>�������֤�����ɻ�õ����֤��������һ�ݡ�������κ��߳���ͼ������֤��
     * ��ѡ��һ����������ոշ��������֤�������̵߳��ȵ�Ŀ�ģ����̱߳�(����)���á�
     *
     * <p>û��Ҫ���ͷ����֤���̱߳���ͨ������acquire()��������֤��
     * �ź�������ȷ�÷���ͨ��Ӧ�ó����еı��Լ����ȷ���ġ�
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * ������ź�����ȡ������������ɣ�����ֱ�����еĶ����ã������̱߳��жϡ�
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * ������ź�����ø�����������ɣ�����ֱ�����еĶ����á�
     *
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * �����������֤�ڵ���ʱ����ʱ���ŴӸ��ź�����ȡ�������������֤��
     *
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * ����ڸ����ĵȴ�ʱ����������ɶ������ҵ�ǰ�߳�û�б��жϣ���Ӹ��ź�����ȡ������������ɡ�
     *
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * �ͷŸ������������֤�������Ƿ��ص��ź�����
     *
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * ���ش��ź����п��õ����֤�ĵ�ǰ������
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    public int availablePermits() {
        return sync.getPermits();
    }

    /**
     * ��ȡ�����������������õ����֤��
     *
     * @return the number of permits acquired
     */
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * ͨ��ָ���ļ��������ٿ������֤��������
     * �˷�����ʹ���ź������ٲ�������Դ�������зǳ����á�
     * ���ַ�����acquire�Ĳ�֮ͬ��������������ֹ���֤�Ļ�ȡ��
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    /**
     * �������ź����Ĺ�ƽ������Ϊ�棬�򷵻��档
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * ��ѯ�Ƿ����߳����ڵȴ���ȡ����ע�⣬��Ϊȡ��������ʱ������
     * һ�������ķ��ز�����֤�κ������߳̽���á��÷�����Ҫ���ڼ��ϵͳ״̬��
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * ���صȴ���ȡ���߳������Ĺ���ֵ�����ֵֻ��һ������ֵ��
     * ��Ϊ��������������ڲ����ݽṹʱ���̵߳��������ܻᶯ̬�仯��
     * �˷���������ڼ���ϵͳ״̬������������ͬ�����ơ�
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * ����һ�������������ڵȴ���ȡ���̵߳ļ��ϡ���Ϊ�ڹ���������ʱ��
     * ʵ�ʵ��̼߳����ܻᶯ̬�仯�����Է��صļ���ֻ��һ�����Ч���Ĺ��ơ�
     * ���ؼ��ϵ�Ԫ��û���ض���˳�����ַ�����Ŀ����Ϊ�˷��㹹���ṩ���㷺�ļ�����ʩ�����ࡣ
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * ���ر�ʶ����ź������ַ�������״̬�������е�״̬�����ַ�����permissions =�������֤������
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
