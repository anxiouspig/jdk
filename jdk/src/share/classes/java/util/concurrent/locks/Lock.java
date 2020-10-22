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

/**
 * ��ʵ���ṩ�˱�ʹ��ͬ�������������㷺����������������������Ľṹ��
 * ���ܾ�����ȫ��ͬ�����ԣ����ҿ���֧�ֶ����������������
 *
 * <p>����һ�ֹ��ߣ����ڿ��ƶ���̶߳Թ�����Դ�ķ��ʡ�
 * ͨ�������ṩ�Թ�����Դ�Ķ�ռ����:һ��ֻ��һ���߳̿��Ի������
 * ���Թ�����Դ�����з��ʶ�Ҫ�����Ȼ���������ǣ���Щ���������������ʹ�����Դ�������д���Ķ�����
 *
 * <p>ʹ��ͬ������������ṩ��ÿ�������������ʽ��������,��ǿ����������ȡ���ͷŷ����������ṹ��ʽ:
 * �ڻ�ö����ʱ,���Ǳ��뱻�ͷ����෴��˳��,�����е����ͷŶ��������ȡ����ͬ�Ĵʷ�������
 *
 * <p>��Ȼͬ���������������������ʹʹ�ü����������б�̱�ø����ף�
 * �������ڱ�������漰���ĳ�����̴��󣬵�����ĳЩ����£�����Ҫ�Ը����ķ�ʽʹ������
 * ���磬һЩ�����������ʵ����ݽṹ���㷨��Ҫʹ�á�hand-over-hand����chain locked��:
 * �Ȼ�ȡ�ڵ�A������Ȼ���ǽڵ�B��Ȼ���ͷ�A����ȡC��Ȼ���ͷ�B����ȡD���ȵȡ�
 * Lock�ӿڵ�ʵ�������ڲ�ͬ�ķ�Χ�ڻ�ȡ���ͷ�����������������˳���ȡ���ͷŶ�������Ӷ�֧��ʹ����Щ������
 *
 * <p>��������Ե����Ӵ����˶�������Ρ���ṹ����ȱʧ������ͬ����������䷢��ʱ�����Զ��ͷš�
 * �ڴ��������£�Ӧ��ʹ������ϰ��:
 *
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // ��ñ�����������Դ
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * �������ͽ��������ڲ�ͬ��������ʱ������ע��ȷ��������ʱִ�е����д��붼�ܵ�try-finally��try-catch�ı�����
 * ��ȷ���ڱ�Ҫʱ�ͷ�����
 *
 * <p>����ʵ���ṩ����Ĺ�����ʹ��ͬ�����������ͨ���ṩһ���������ĳ��Ի��һ����(tryLock()),
 * ��ͼ�����,���Դ��(lockInterruptibly(),����ͼ�������ʱ(tryLock(long,TimeUnit))��
 *
 * <p>���໹�����ṩ����ʽ����������ȫ��ͬ����Ϊ�����壬�籣֤˳�򡢲�������ʹ�û�������⡣
 * ���ʵ���ṩ������ר�ŵ����壬��ôʵ�ֱ����¼��Щ���塣
 *
 * <p>��ע�⣬��ʵ��ֻ����ͨ�������Ǳ����������ͬ������е�Ŀ�ꡣ
 * ��ȡ��ʵ���ļ�����������ø�ʵ�����κ�lock()����û��ָ���Ĺ�ϵ��
 * Ϊ�˱����������������Զ��Ҫ�����ַ�ʽʹ����ʵ���������������Լ���ʵ���С�
 *
 * <p>�����ر�ָ����Ϊ�κβ�������nullֵ���ᵼ���׳�NullPointerException��
 *
 * <h3>Memory Synchronization</h3>
 *
 * <p>���е���ʵ�ֶ�����ִ�������ü��������ṩ����ͬ���ڴ�ͬ�����壬��Java���Թ淶(17.4�ڴ�ģ��)������:
 * <li>�ɹ�����������ɹ�������Ϊ������ͬ���ڴ�ͬ��Ч����
 * <li>�ɹ��Ľ���������ɹ��Ľ�����Ϊ������ͬ���ڴ�ͬ��Ч����
 * </ul>
 *
 * ���ɹ��������ͽ��������Լ������������/������������Ҫ�κ��ڴ�ͬ��Ч����
 *
 * <h3>Implementation Considerations</h3>
 *
 * <p>������ʽ������ȡ(���ж����������ж����Ͷ�ʱ)������������˳��֤������ʵ�������������������ͬ��
 * ���⣬�ж����ڽ��е�����ȡ�����������ڸ����������в����á�
 * ��ˣ�ʵ�ֲ���ҪΪ����������ʽ������ȡ������ȫ��ͬ�ı�֤�����壬Ҳ����Ҫ֧�����ڽ��е�����ȡ���жϡ�
 * ��Ҫһ��ʵ��������ؼ�¼ÿ�����������ṩ������ͱ�֤��
 * ����������������ӿ��ж�����ж����壬ǰ����֧������ȡ���ж�:Ҫô��ȫ֧�֣�Ҫôֻ֧�ַ�����ڡ�
 *
 * <p>�����ж�ͨ����ζ��ȡ�������Ҷ��жϵļ��ͨ����Ƶ����
 * ����ʵ�ֿ��ܸ���������Ӧ�жϣ������������ķ������ء�
 * ��ʹ������ʾ����һ������֮�������жϿ����Ѿ�������߳���������Ҳ����ȷ�ġ�ʵ��Ӧ�ü�¼������Ϊ��
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * �����.
     *
     * <p>����������ã���ǰ�߳̽������̵߳���Ŀ�Ķ����ã�����������״̬��ֱ�������Ϊֹ��
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>��ʵ�ֿ����ܹ�������Ĵ���ʹ�ã����絼�������ĵ��ã�������������������׳�(δѡ�е�)�쳣��
     * ����ʵ�ֱ����¼�������쳣���͡�
     */
    void lock();

    /**
     * ���ǵ�ǰ�߳��жϣ������ȡ����
     *
     * <p>��������ã����ȡ�����������ء�
     *
     * <p>����������ã���ô��ǰ�߳̽������̵߳��ȵ�Ŀ�ı����ã�����������״̬��ֱ�����������������֮һ:
     *
     * <ul>
     * <li>������ǰ�̻߳�ȡ;
     * <li>����һЩ�߳��жϵ�ǰ�̣߳�֧���ж�����ȡ��
     * </ul>
     *
     * <p>�����ǰ�̣߳�
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>��ȡ��ʱ�жϣ�֧������ȡ�жϣ�Ȼ���׳�InterruptedException���������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>��ĳЩʵ�����ж�����ȡ�����������ǲ����ܵģ�����������ܵĻ���
     * �������һ������Ĳ���������ԱӦ����ʶ����������������ġ�����������£�ʵ��Ӧ�ü�¼��
     *
     * <p>ʵ�ֿ��ܸ���������Ӧ�ж϶����������ķ������ء�
     *
     * <p>��ʵ�ֿ����ܹ�������Ĵ���ʹ�ã����絼�������ĵ��ã�������������������׳�(δѡ�е�)�쳣��
     * ����ʵ�ֱ����¼�������쳣���͡�
     *
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * �������ڵ���ʱ���ڿ���״̬ʱ�Ż�ȡ����
     *
     * <p>��������ã����ȡ������������ֵtrue��
     * ����������ã���˷�������������ֵfalse��
     *
     * <p>���ַ����ĵ����÷���:
     *  <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     *
     * �����÷�ȷ�����ڱ���ȡʱ������������δ����ȡʱ���᳢�Խ�����
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    boolean tryLock();

    /**
     * ����ڸ����ĵȴ�ʱ�����ǿ��еĲ��ҵ�ǰ�߳�û�б��жϣ����ȡ����
     *
     * <p>��������ã���˷�����������ֵtrue������������ã���ô��ǰ�߳̽������̵߳��ȵ�Ŀ�Ķ������ã�
     * ����������״̬��ֱ�����������������֮һ:
     * <ul>
     * <li>������ǰ�̻߳�ȡ;
     * <li>�����߳��жϵ�ǰ�̣߳�֧������ȡ�ж�;
     * <li>ָ���ĵȴ�ʱ���Ѿ�����
     * </ul>
     *
     * <p>�����������򷵻�trueֵ��
     *
     * <p>�����ǰ�߳�:
     * <ul>
     * <li>�ڽ���˷���ʱ���������ж�״̬;
     * <li>�ڻ�ȡ��ʱ�жϣ�����֧�ֻ�ȡ�����жϣ�Ȼ���׳�InterruptedException�������ǰ�̵߳��ж�״̬��
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>���ָ���ĵȴ�ʱ����ڣ��򷵻�falseֵ�����ʱ��С�ڻ����0����÷�����������ȴ���
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>��ĳЩʵ�����ж�����ȡ�����������ǲ����ܵģ�����������ܵĻ����������һ������Ĳ�����
     * ����ԱӦ����ʶ����������������ġ�����������£�ʵ��Ӧ�ü�¼��
     *
     * <p>ʵ�ֿ��ܸ���������Ӧ�ж϶����������ķ������أ����߱��泬ʱ��
     *
     * <p>��ʵ�ֿ����ܹ�������Ĵ���ʹ�ã����絼�������ĵ��ã�������������������׳�(δѡ�е�)�쳣��
     * ����ʵ�ֱ����¼�������쳣���͡�
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>��ʵ��ͨ������ĸ��߳̿����ͷ���ʩ������(ͨ��ֻ�����ĳ����߿����ͷ���)�����Υ�������ƣ�
     * ���ܻ��׳�(δѡ�е�)�쳣���κ����ƺ��쳣���Ͷ���������ʵ�ּ�¼������
     */
    void unlock();

    /**
     * ���ذ󶨵�����ʵ����������ʵ����
     *
     * <p>�ڵȴ�����֮ǰ���������ɵ�ǰ�̳߳��С���Condition.await()�ĵ��ý��Զ����ڵȴ�֮ǰ�ͷ�����
     * ���ڵȴ�����֮ǰ���»�ȡ����
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>����ʵ����ȷ�в���ȡ������ʵ�֣����ұ����ɸ�ʵ�ּ�¼��
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
