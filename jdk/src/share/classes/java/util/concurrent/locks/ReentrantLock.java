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

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * �����뻥������������ʹ��ͬ�������������ʵ���ʽ����������ͬ�Ļ�����Ϊ�����壬��������չ���ܡ�
 *
 * <p>ReentrantLock�����һ�γɹ��������߳�ӵ�У�����δ��������������������һ���߳�ʱ��
 * ���������߳̽����أ����ɹ���ȡ���������ǰ�߳��Ѿ�ӵ��������÷������������ء�
 * ����ʹ�÷���isHeldByCurrentThread()��getHoldCount()���м�顣
 *
 * <p>����Ĺ��캯������һ����ѡ�Ĺ�ƽ�Բ�����������Ϊ��ʱ��������״̬�£������������������ȴ��̵߳ķ���Ȩ��
 * ���򣬴�������֤�κ��ض��ķ���˳��ʹ�ö���̷߳��ʵĹ�ƽ���ĳ�����ܻ���ʾ�ϵ͵�����������
 * (������;ͨ������Щʹ��Ĭ�����õ�Ҫ���ö�)�������ڻ�����ͱ�֤������������ʱ������С��
 * ������ע�⣬���Ĺ�ƽ�Բ�����֤�̵߳��ȵĹ�ƽ�ԡ���ˣ�ʹ�ù�ƽ���Ķ���߳��е�һ�����ܻ�������λ������
 * ��������߳�û�н�չ��Ҳû�е�ǰ����������Ҫע�⣬����ʱ��tryLock()������֧�ֹ�ƽ�����á�
 * ��������ã���ʹ�����߳����ڵȴ�����Ҳ��ɹ���
 *
 * <p>����������ǣ�������������һ������������һ��try�飬����͵�����һ��ǰ��ṹ����:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * <p>����ʵ�����ӿ�֮�⣬���໹������������ڼ����״̬�Ĺ����������ܱ�������������һЩ�������Լ��ͼ������á�
 *
 * <p>���������л�������������Ϊ��ʽ��ͬ:�����л��������ڽ���״̬�������������л�ʱ��״̬�޹ء�
 *
 * <p>�������֧��ͬһ�̵߳�2147483647���ݹ�������ͼ���������ƽ��������������׳�����
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    /** �ṩ����ʵ�ֹ��ܵ�ͬ�� */
    private final Sync sync;

    /**
     * �������ͬ�����ƻ��������໯Ϊ��ƽ�ͷǹ�ƽ�İ汾���档ʹ��AQS״̬��ʾ���ϵĳ�������
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        /**
         * Performs {@link Lock#lock}. ���໯����Ҫԭ��������ǹ�ƽ�汾�Ŀ���·����
         */
        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire ������ʵ�֡�
         * but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            // �õ���ǰ�߳�
            int c = getState();
            // �õ�����״̬����ʼ��Ϊ0
            if (c == 0) {
                // ��Ϊ0������ӵ����
                if (compareAndSetState(0, acquires)) { // ��״̬��Ϊacquires
                    setExclusiveOwnerThread(current); // ����������
                    return true; // ���سɹ�
                }
            }
            else if (current == getExclusiveOwnerThread()) { // ��ǰ�߳��ǲ������������߳�
                int nextc = c + acquires; // ��ֵ
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc); // ����״ֵ̬
                return true; // ����
            }
            return false; // �õ���ʧ��
        }

        // �ͷ���
        protected final boolean tryRelease(int releases) {
            int c = getState() - releases; // ��ֵ
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException(); // ��ǰ�̲߳�Ϊ���������̵߳Ļ��׳��쳣
            boolean free = false;
            if (c == 0) { // ״̬=0����������������Ϊnull
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c); // ����״̬
            return free; // ���ؽ��
        }

        // �Ƿ����������
        protected final boolean isHeldExclusively() {
            // ����һ����ӵ����֮ǰ��ȡ״̬,
            // �����ǰ�߳�ӵ���������ǲ���Ҫȥ���
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
        // ������
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // �������ⲿ��ķ���

        // ��ǰ���������߳�
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }
        // �õ�����������
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }
        // �Ƿ�����״̬
        final boolean isLocked() {
            return getState() != 0;
        }

        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state
        }
    }

    /**
     * �ǹ�ƽ����ͬ������
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        /**
         * ִ����. Try immediate barge, backing up to normal
         * acquire on failure.
         */
        final void lock() { // ��state0��Ϊ1��ʧ�ܵĻ�ȥ��ȡ��
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) { // ��ͼ��ȡ��
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * ��ƽ����ͬ������
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() { // ��ͼ��ȡ��
            acquire(1);
        }

        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread(); // �õ���ǰ�߳�
            int c = getState(); // �õ���ǰ��״̬
            if (c == 0) { // Ϊ0���Ի�ȡ��
                if (!hasQueuedPredecessors() && // �Ƿ���ǰ��
                    compareAndSetState(0, acquires)) { // ��ǰ�߳�����
                    setExclusiveOwnerThread(current); // ���õ�ǰ�߳�Ϊ�������߳�
                    return true; // ���ػ�����ɹ�
                }
            }
            else if (current == getExclusiveOwnerThread()) { // ��ǰ�����Ƿ����������߳�
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true; // ������ɹ�
            }
            return false; // ��������ʧ��
        }
    }

    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * Acquires the lock.
     *
     * <p>��������������̳߳��У����ȡ�����������أ��������м�������Ϊ1��
     *
     * <p>�����ǰ�߳��Ѿ�����������ô���м���������1�������������ء�
     *
     * <p>���������һ���̳߳��У���ô��ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ����ã�����������״̬��
     * ֱ��������ȡ����ʱ�����м���������Ϊ1��
     */
    public void lock() {
        sync.lock();
    }

    /**
     * ���ǵ�ǰ�߳��жϣ������ȡ����
     *
     * <p>��������������̳߳��У����ȡ�����������أ��������м�������Ϊ1��
     *
     * <p>�����ǰ�߳��Ѿ����и�������ô���м���������1���÷����������ء�
     *
     * <p>�����������һ���̳߳��У���ô��ǰ�߳̾ͻ�����̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     *
     * <li>������ǰ�̻߳�ȡ;
     *
     * <li>����һЩ�߳��жϵ�ǰ�̡߳�
     *
     * </ul>
     *
     * <p>���������ǰ�̻߳�ȡ����ô�����м���������Ϊ1��
     *
     * <p>�����ǰ�߳�:
     *
     * <ul>
     *
     * <li>�ڽ���˷���ʱ�������ж�״̬;
     *
     * <li>�ڻ�ȡ��ʱ�жϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>�����ʵ���У��������������һ����ʽ���жϵ㣬����������Ӧ�ж϶����������Ļ�����������ȡ��
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * �������ڵ���ʱ���������̳߳���ʱ���Ż�ȡ����
     *
     * <p>�����δ�������̳߳��У����ȡ��������������ֵtrue���������м�������Ϊ1��
     * ��ʹ�����������Ϊʹ�ù�ƽ��������ԣ���������ã�����tryLock()Ҳ�������������
     * ���������߳��Ƿ����ڵȴ��������֡���ײ����Ϊ��ĳЩ����������õģ���ʹ���ƻ��˹�ƽ��
     * �������Ϊ�����ִ�й�ƽ���ã���ôʹ��tryLock(0, TimeUnit.SECONDS)���⼸���ǵȼ۵�(�������Լ�⵽�ж�)��
     *
     * <p>�����ǰ�߳��Ѿ����и�������ô���м���������1����������true��
     *
     * <p>���������һ���̳߳��У���ô�����������������ֵfalse��
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * ����ڸ����ĵȴ�ʱ����û�б������̳߳��У����ҵ�ǰ�߳�û�б��жϣ����ȡ����
     *
     * <p>�����δ�������̳߳��У����ȡ��������������ֵtrue���������м�������Ϊ1��
     * ��������������Ϊʹ�ù�ƽ��������ԣ���ô����κ������߳����ڵȴ����������ô��������һ�����õ�����
     * ����tryLock()�����෴���������Ҫһ����ʱ��tryLock�������һ����ƽ������
     * Ȼ������һ���ʱ��Ͳ���ʱ����ʽ:
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * <p>�����ǰ�߳��Ѿ����и�������ô���м���������1����������true��
     *
     * <p>�����������һ���̳߳��У���ô��ǰ�߳̾ͻ�����̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     *
     * <li>������ǰ�̻߳�ȡ;
     *
     * <li>����һЩ�߳��жϵ�ǰ�߳�;
     *
     * <li>ָ���ĵȴ�ʱ���Ѿ�����
     *
     * </ul>
     *
     * <p>�����ȡ�������򷵻�trueֵ�����������м�������Ϊ1��
     *
     * <p>�����ǰ�߳�:
     *
     * <ul>
     *
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     *
     * <li>�ڻ�ȡ��ʱ�жϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>���ָ���ĵȴ�ʱ����ڣ��򷵻�falseֵ�����ʱ��С�ڻ����0����÷�����������ȴ���
     *
     * <p>�����ʵ���У��������������һ����ʽ���жϵ㣬����������Ӧ�жϣ������������Ļ������Ļ�ȡ����
     * ���߱���ȴ�ʱ������š�
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     * @return {@code true} ������ǿ��еĲ��ұ���ǰ�̻߳�ȡ���������Ѿ�����ǰ�̳߳��У���Ϊtrue;
     * ����ڻ�ȡ��֮ǰ�ĵȴ�ʱ���Ѿ���ȥ����Ϊfalse
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * ��ͼ�ͷŴ�����
     *
     * <p>�����ǰ�߳���������ĳ����ߣ���ô���м������ݼ���������м�������Ϊ�㣬���ͷ�����
     * �����ǰ�̲߳���������ĳ����ߣ���ô�׳�IllegalMonitorStateException��
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * �������ڴ���ʵ��������ʵ����
     *
     * <p>�������õļ�������һ��ʹ��ʱ�����ص�����ʵ��֧����������������(wait��notify��notifyAll)��ͬ���÷���
     *
     * <ul>
     *
     * <li>����ڵ����κ������ȴ������źŷ���ʱδ���д��������׳�IllegalMonitorStateException��
     *
     * <li>�������ȴ�����������ʱ�������ͷţ������Ƿ���֮ǰ���������»�ã������м����ָ�������������ʱ��ֵ��
     *
     * <li>����߳��ڵȴ��ڼ��жϣ���ô�ȴ�����ֹ���׳�InterruptedException��������̵߳��ж�״̬��
     *
     * <li> �ȴ��̰߳�FIFO˳�򷢳��źš�
     *
     * <li>�ӵȴ��������ص��̵߳����ػ�˳���������ȡ�����߳���ͬ(��Ĭ�������δָ��)�������ڹ�ƽ����
     * ����ʹ����Щ�ȴ�ʱ������̡߳�
     *
     * </ul>
     *
     * @return the Condition object
     */
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * ��ѯ��ǰ�̳߳��д����Ĵ�����
     *
     * <p>һ���̶߳�ÿ��������������һ��������ÿ������������һ������������ƥ�䡣
     *
     * <p>hold count��Ϣͨ�������ڲ��Ժ͵���Ŀ�ġ����磬���һ���ض��Ĵ���β�Ӧ�����Ѿ����е���һ�����룬
     * ��ô���ǿ��Զ��������ʵ:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * ��ѯ��ǰ�߳��Ƿ���д�����
     *
     * <p>���������ü���������Thread.holdsLock(Object)�������ƣ��˷���ͨ�����ڵ��ԺͲ��ԡ�
     * ���磬ֻ������������ʱ��Ӧ�õ��õķ������Զ���:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * <p>��Ҳ��������ȷ�����������Բ�������ķ�ʽʹ�ã�����:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    /**
     * ��ѯ�����Ƿ����κ��̳߳��С��˷���������ڼ���ϵͳ״̬������������ͬ�����ơ�
     *
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    public boolean isLocked() {
        return sync.isLocked();
    }

    /**
     * ��������Ĺ�ƽ������Ϊ�棬�򷵻��档
     *
     * @return {@code true} if this lock has fairness set true
     */
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * ���ص�ǰӵ�д������̣߳������ӵ���򷵻�null�����������ߵ��̵߳��ô˷���ʱ��
     * ����ֵ��ӳ��ǰ��״̬�����״̬����ֵ�����磬�����߿�����ʱΪ�գ�
     * ��ʹ���߳���ͼ��ȡ��������δ���������˷�����Ŀ����Ϊ�˷��㹹���ṩ���㷺����������ʩ�����ࡣ
     *
     * @return the owner, or {@code null} if not owned
     */
    protected Thread getOwner() {
        return sync.getOwner();
    }

    /**
     * ��ѯ�Ƿ����߳����ڵȴ���ȡ������ע�⣬��Ϊȡ���������κ�ʱ������
     * һ�������ķ��ز�����֤�κ������߳̽������������÷�����Ҫ���ڼ��ϵͳ״̬��
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    /**
     * ��ѯ�����߳��Ƿ����ڵȴ���ȡ������ע�⣬��Ϊȡ��������ʱ���������������ķ��ز�����֤����߳̽�����������
     * �÷�����Ҫ���ڼ��ϵͳ״̬��
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    /**
     * ���صȴ���ȡ�������߳������Ĺ���ֵ�����ֵֻ��һ������ֵ����Ϊ��������������ڲ����ݽṹʱ��
     * �̵߳��������ܻᶯ̬�仯���˷���������ڼ���ϵͳ״̬������������ͬ�����ơ�
     *
     * @return the estimated number of threads waiting for this lock
     */
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    /**
     * ����һ�������������ڵȴ���ȡ�������̵߳ļ��ϡ���Ϊ�ڹ���������ʱ��ʵ�ʵ��̼߳����ܻᶯ̬�仯��
     * ���Է��صļ���ֻ��һ�����Ч���Ĺ��ơ����ؼ��ϵ�Ԫ��û���ض���˳��
     * ���ַ�����Ŀ����Ϊ�˷��㹹���ṩ���㷺�ļ�����ʩ�����ࡣ
     *
     * @return the collection of threads
     */
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    /**
     * ��ѯ�Ƿ����߳����ڵȴ�����������ĸ���������ע�⣬��Ϊ��ʱ���жϿ�����ʱ������
     * ���������ķ��ز�����֤�������źŽ������κ��̡߳��÷�����Ҫ���ڼ��ϵͳ״̬��
     *
     * @param condition the condition
     * @return ������κ����ڵȴ����̣߳���Ϊ��
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * ��������������ĸ��������µȴ����߳����Ĺ���ֵ��
     * ��ע�⣬���ڳ�ʱ���жϿ�����ʱ��������˹���ֵ����Ϊʵ�ʵȴ������������ޡ�
     * �˷���������ڼ���ϵͳ״̬������������ͬ�����ơ�
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * ����һ�����ϣ����а����������ڵȴ�����������ĸ����������̡߳���Ϊ�ڹ���������ʱ��
     * ʵ�ʵ��̼߳����ܻᶯ̬�仯�����Է��صļ���ֻ��һ�����Ч���Ĺ��ơ�
     * ���ؼ��ϵ�Ԫ��û���ض���˳�����ַ�����Ŀ����Ϊ�˷��㹹���ṩ���㷺��״̬������ʩ�����ࡣ
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    /**
     * ���ر�ʶ�������ַ���������״̬�������е�״̬�����ַ��������������ַ�����Locked by������������̵߳����ơ�
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
