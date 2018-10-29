/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.cache.decorators;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Weak Reference cache decorator.
 * Thanks to Dr. Heinz Kabutz for his guidance here.
 * 基于 java.lang.ref.WeakReference 的 Cache 实现类
 * 4种引用类型文章推荐https://juejin.im/post/5a5129f5f265da3e317dfc08
 * @author Clinton Begin
 */
public class WeakCache implements Cache {
  /**
   * 强引用的键的队列
   */
  private final Deque<Object> hardLinksToAvoidGarbageCollection;
  /**
   * 引用队列
   */
  private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
  /**
   * 委托的 Cache 对象
   */
  private final Cache delegate;
  /**
   * {@link #hardLinksToAvoidGarbageCollection} 的大小
   */
  private int numberOfHardLinks;

  public WeakCache(Cache delegate) {
    this.delegate = delegate;
    this.numberOfHardLinks = 256;
    this.hardLinksToAvoidGarbageCollection = new LinkedList<>();
    this.queueOfGarbageCollectedEntries = new ReferenceQueue<>();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    // 移除已经被 GC 回收的 WeakEntry
    removeGarbageCollectedItems();
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.numberOfHardLinks = size;
  }

  @Override
  public void putObject(Object key, Object value) {
    // 移除已经被 GC 回收的 WeakEntry
    removeGarbageCollectedItems();
    // 添加到 delegate 中
    //WeakEntry 对象的 garbageCollectionQueue 属性为 queueOfGarbageCollectedEntries 。
    // 也就说，如果 WeakEntry 对象被 GC 时，就会添加到 queueOfGarbageCollectedEntries 队列中
    delegate.putObject(key, new WeakEntry(key, value, queueOfGarbageCollectedEntries));
  }

  @Override
  public Object getObject(Object key) {
    Object result = null;
    @SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
    // 获得值的 WeakReference 对象
    WeakReference<Object> weakReference = (WeakReference<Object>) delegate.getObject(key);
    if (weakReference != null) {
      // 获得值
      result = weakReference.get();
      // 为空，从 delegate 中移除 。为空的原因是，意味着已经被 GC 回收
      if (result == null) {
        delegate.removeObject(key);
      } else { // 非空，添加到 hardLinksToAvoidGarbageCollection 中，避免被 GC
        // 添加到 hardLinksToAvoidGarbageCollection 的队头
        //为了避免被 GC 掉，所以添加到 hardLinksToAvoidGarbageCollection 队头。
        // 但是，该队列设置了一个上限( numberOfHardLinks )，避免队列无限大
        //另外，这里添加到 hardLinksToAvoidGarbageCollection 队头应该是有问题的。因为，可能存在重复添加，如果获取相同的键
        hardLinksToAvoidGarbageCollection.addFirst(result);
        // 超过上限，移除 hardLinksToAvoidGarbageCollection 的队尾
        if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
          hardLinksToAvoidGarbageCollection.removeLast();
        }
      }
    }
    return result;
  }

  @Override
  public Object removeObject(Object key) {
    // 移除已经被 GC 回收的 WeakEntry
    removeGarbageCollectedItems();
    // 移除出 delegate
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    // 清空 hardLinksToAvoidGarbageCollection
    hardLinksToAvoidGarbageCollection.clear();
    // 移除已经被 GC 回收的 WeakEntry
    removeGarbageCollectedItems();
    // 清空 delegate
    delegate.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  private void removeGarbageCollectedItems() {
    WeakEntry sv;
    while ((sv = (WeakEntry) queueOfGarbageCollectedEntries.poll()) != null) {
      delegate.removeObject(sv.key);
    }
  }

  private static class WeakEntry extends WeakReference<Object> {
    private final Object key;

    private WeakEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
      super(value, garbageCollectionQueue);
      this.key = key;
    }
  }

}
