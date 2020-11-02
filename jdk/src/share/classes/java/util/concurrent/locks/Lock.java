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
 * 锁实现提供了比使用同步方法和语句更广泛的锁操作。它们允许更灵活的结构，
 * 可能具有完全不同的属性，并且可能支持多个关联的条件对象。
 *
 * <p>锁是一种工具，用于控制多个线程对共享资源的访问。
 * 通常，锁提供对共享资源的独占访问:一次只有一个线程可以获得锁，
 * 而对共享资源的所有访问都要求首先获得锁。但是，有些锁可能允许并发访问共享资源，比如读写锁的读锁。
 *
 * <p>使用同步方法或语句提供与每个对象关联的隐式监视器锁,但强制所有锁获取和释放发生在阻塞结构方式:
 * 在获得多个锁时,它们必须被释放在相反的顺序,和所有的锁释放都必须与获取在相同的词法作用域。
 *
 * <p>虽然同步方法和语句的作用域机制使使用监视器锁进行编程变得更容易，
 * 并有助于避免许多涉及锁的常见编程错误，但是在某些情况下，您需要以更灵活的方式使用锁。
 * 例如，一些遍历并发访问的数据结构的算法需要使用“hand-over-hand”或“chain locked”:
 * 先获取节点A的锁，然后是节点B，然后释放A并获取C，然后释放B并获取D，等等。
 * Lock接口的实现允许在不同的范围内获取和释放锁，并允许以任意顺序获取和释放多个锁，从而支持使用这些技术。
 *
 * <p>这种灵活性的增加带来了额外的责任。块结构锁的缺失消除了同步方法和语句发生时锁的自动释放。
 * 在大多数情况下，应该使用以下习语:
 *
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // 获得被锁保护的资源
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * 当锁定和解锁发生在不同的作用域时，必须注意确保持有锁时执行的所有代码都受到try-finally或try-catch的保护，
 * 以确保在必要时释放锁。
 *
 * <p>锁的实现提供额外的功能在使用同步方法和语句通过提供一个非阻塞的尝试获得一个锁(tryLock()),
 * 企图获得锁,可以打断(lockInterruptibly(),和试图获得锁超时(tryLock(long,TimeUnit))。
 *
 * <p>锁类还可以提供与隐式监视器锁完全不同的行为和语义，如保证顺序、不可重入使用或死锁检测。
 * 如果实现提供了这种专门的语义，那么实现必须记录这些语义。
 *
 * <p>请注意，锁实例只是普通对象，它们本身可以用作同步语句中的目标。
 * 获取锁实例的监视器锁与调用该实例的任何lock()方法没有指定的关系。
 * 为了避免混淆，建议您永远不要以这种方式使用锁实例，除非在它们自己的实现中。
 *
 * <p>除非特别指出，为任何参数传递null值都会导致抛出NullPointerException。
 *
 * <h3>Memory Synchronization</h3>
 *
 * <p>所有的锁实现都必须执行与内置监视器锁提供的相同的内存同步语义，如Java语言规范(17.4内存模型)中所述:
 * <li>成功的锁操作与成功的锁行为具有相同的内存同步效果。
 * <li>成功的解锁操作与成功的解锁行为具有相同的内存同步效果。
 * </ul>
 *
 * 不成功的锁定和解锁操作以及可重入的锁定/解锁操作不需要任何内存同步效果。
 *
 * <h3>Implementation Considerations</h3>
 *
 * <p>三种形式的锁获取(可中断续、不可中断续和定时)在性能特征、顺序保证或其他实现质量方面可能有所不同。
 * 此外，中断正在进行的锁获取的能力可能在给定的锁类中不可用。
 * 因此，实现不需要为所有三种形式的锁获取定义完全相同的保证或语义，也不需要支持正在进行的锁获取的中断。
 * 需要一个实现来清楚地记录每个锁定方法提供的语义和保证。
 * 它还必须遵守这个接口中定义的中断语义，前提是支持锁获取的中断:要么完全支持，要么只支持方法入口。
 *
 * <p>由于中断通常意味着取消，而且对中断的检查通常不频繁，
 * 所以实现可能更倾向于响应中断，而不是正常的方法返回。
 * 即使可以显示在另一个操作之后发生的中断可能已经解除了线程阻塞，这也是正确的。实现应该记录这种行为。
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
     * 获得锁.
     *
     * <p>如果锁不可用，则当前线程将出于线程调度目的而禁用，并处于休眠状态，直到获得锁为止。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>锁实现可能能够检测锁的错误使用，例如导致死锁的调用，并可能在这种情况下抛出(未选中的)异常。
     * 该锁实现必须记录环境和异常类型。
     */
    void lock();

    /**
     * 除非当前线程中断，否则获取锁。
     *
     * <p>如果锁可用，则获取锁并立即返回。
     *
     * <p>如果锁不可用，那么当前线程将出于线程调度的目的被禁用，并处于休眠状态，直到发生以下两种情况之一:
     *
     * <ul>
     * <li>锁被当前线程获取;
     * <li>其他一些线程中断当前线程，支持中断锁获取。
     * </ul>
     *
     * <p>如果当前线程：
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>获取锁时中断，支持锁获取中断，然后抛出InterruptedException，并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>在某些实现中中断锁获取的能力可能是不可能的，而且如果可能的话，
     * 这可能是一个昂贵的操作。程序员应该意识到情况可能是这样的。在这种情况下，实现应该记录。
     *
     * <p>实现可能更倾向于响应中断而不是正常的方法返回。
     *
     * <p>锁实现可能能够检测锁的错误使用，例如导致死锁的调用，并可能在这种情况下抛出(未选中的)异常。
     * 该锁实现必须记录环境和异常类型。
     *
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 仅当锁在调用时处于空闲状态时才获取锁。
     *
     * <p>如果锁可用，则获取锁并立即返回值true。
     * 如果锁不可用，则此方法将立即返回值false。
     *
     * <p>这种方法的典型用法是:
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
     * 这种用法确保锁在被获取时被解锁，而在未被获取时不会尝试解锁。
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    boolean tryLock();

    /**
     * 如果在给定的等待时间内是空闲的并且当前线程没有被中断，则获取锁。
     *
     * <p>如果锁可用，则此方法立即返回值true。如果锁不可用，那么当前线程将出于线程调度的目的而被禁用，
     * 并处于休眠状态，直到发生以下三种情况之一:
     * <ul>
     * <li>锁被当前线程获取;
     * <li>其他线程中断当前线程，支持锁获取中断;
     * <li>指定的等待时间已经过了
     * </ul>
     *
     * <p>如果获得锁，则返回true值。
     *
     * <p>如果当前线程:
     * <ul>
     * <li>在进入此方法时已设置其中断状态;
     * <li>在获取锁时中断，并且支持获取锁的中断，然后抛出InterruptedException并清除当前线程的中断状态。
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>如果指定的等待时间过期，则返回false值。如果时间小于或等于0，则该方法根本不会等待。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>在某些实现中中断锁获取的能力可能是不可能的，而且如果可能的话，这可能是一个昂贵的操作。
     * 程序员应该意识到情况可能是这样的。在这种情况下，实现应该记录。
     *
     * <p>实现可能更倾向于响应中断而不是正常的方法返回，或者报告超时。
     *
     * <p>锁实现可能能够检测锁的错误使用，例如导致死锁的调用，并可能在这种情况下抛出(未选中的)异常。
     * 该锁实现必须记录环境和异常类型。
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
     * <p>锁实现通常会对哪个线程可以释放锁施加限制(通常只有锁的持有者可以释放锁)，如果违反了限制，
     * 可能会抛出(未选中的)异常。任何限制和异常类型都必须由锁实现记录下来。
     */
    void unlock();

    /**
     * 返回绑定到此锁实例的新条件实例。
     *
     * <p>在等待条件之前，锁必须由当前线程持有。对Condition.await()的调用将自动地在等待之前释放锁，
     * 并在等待返回之前重新获取锁。
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>条件实例的确切操作取决于锁实现，并且必须由该实现记录。
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    Condition newCondition();
}
