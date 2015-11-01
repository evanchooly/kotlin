@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("SequencesKt")

package kotlin.collections

//
// NOTE THIS FILE IS AUTO-GENERATED by the GenerateStandardLib.kt
// See: https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib
//

import java.util.*

import java.util.Collections // TODO: it's temporary while we have java.util.Collections in js

/**
 * Returns `true` if [element] is found in the collection.
 */
public operator fun <T> Sequence<T>.contains(element: @kotlin.internal.NoInfer T): Boolean {
    return indexOf(element) >= 0
}

/**
 * Returns `true` if [element] is found in the collection.
 */
@Deprecated("Use 'containsRaw' instead.", ReplaceWith("containsRaw(element)"))
@kotlin.jvm.JvmName("containsAny")
@kotlin.internal.LowPriorityInOverloadResolution
public operator fun <T> Sequence<T>.contains(element: T): Boolean {
    return containsRaw(element)
}

/**
 * Returns `true` if [element] is found in the collection.
 * Allows to overcome type-safety restriction of `contains` that requires to pass an element of type `T`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Sequence<*>.containsRaw(element: Any?): Boolean {
    return contains<Any?>(element)
}

/**
 * Returns an element at the given [index] or throws an [IndexOutOfBoundsException] if the [index] is out of bounds of this collection.
 */
public fun <T> Sequence<T>.elementAt(index: Int): T {
    return elementAtOrElse(index) { throw IndexOutOfBoundsException("Sequence doesn't contain element at index $index.") }
}

/**
 * Returns an element at the given [index] or the result of calling the [defaultValue] function if the [index] is out of bounds of this collection.
 */
public fun <T> Sequence<T>.elementAtOrElse(index: Int, defaultValue: (Int) -> T): T {
    if (index < 0)
        return defaultValue(index)
    val iterator = iterator()
    var count = 0
    while (iterator.hasNext()) {
        val element = iterator.next()
        if (index == count++)
            return element
    }
    return defaultValue(index)
}

/**
 * Returns an element at the given [index] or `null` if the [index] is out of bounds of this collection.
 */
public fun <T> Sequence<T>.elementAtOrNull(index: Int): T? {
    if (index < 0)
        return null
    val iterator = iterator()
    var count = 0
    while (iterator.hasNext()) {
        val element = iterator.next()
        if (index == count++)
            return element
    }
    return null
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun <T> Sequence<T>.find(predicate: (T) -> Boolean): T? {
    return firstOrNull(predicate)
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Sequence<T>.findLast(predicate: (T) -> Boolean): T? {
    return lastOrNull(predicate)
}

/**
 * Returns first element.
 * @throws [NoSuchElementException] if the collection is empty.
 */
public fun <T> Sequence<T>.first(): T {
    val iterator = iterator()
    if (!iterator.hasNext())
        throw NoSuchElementException("Sequence is empty.")
    return iterator.next()
}

/**
 * Returns the first element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Sequence<T>.first(predicate: (T) -> Boolean): T {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("No element matching predicate was found.")
}

/**
 * Returns the first element, or `null` if the collection is empty.
 */
public fun <T> Sequence<T>.firstOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext())
        return null
    return iterator.next()
}

/**
 * Returns the first element matching the given [predicate], or `null` if element was not found.
 */
public inline fun <T> Sequence<T>.firstOrNull(predicate: (T) -> Boolean): T? {
    for (element in this) if (predicate(element)) return element
    return null
}

/**
 * Returns first index of [element], or -1 if the collection does not contain element.
 */
public fun <T> Sequence<T>.indexOf(element: @kotlin.internal.NoInfer T): Int {
    var index = 0
    for (item in this) {
        if (element == item)
            return index
        index++
    }
    return -1
}

/**
 * Returns first index of [element], or -1 if the collection does not contain element.
 */
@Deprecated("Use 'indexOfRaw' instead.", ReplaceWith("indexOfRaw(element)"))
@kotlin.jvm.JvmName("indexOfAny")
@kotlin.internal.LowPriorityInOverloadResolution
@Suppress("NOTHING_TO_INLINE")
public fun <T> Sequence<T>.indexOf(element: T): Int {
    return indexOfRaw(element)
}

/**
 * Returns index of the first element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Sequence<T>.indexOfFirst(predicate: (T) -> Boolean): Int {
    var index = 0
    for (item in this) {
        if (predicate(item))
            return index
        index++
    }
    return -1
}

/**
 * Returns index of the last element matching the given [predicate], or -1 if the collection does not contain such element.
 */
public inline fun <T> Sequence<T>.indexOfLast(predicate: (T) -> Boolean): Int {
    var lastIndex = -1
    var index = 0
    for (item in this) {
        if (predicate(item))
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns first index of [element], or -1 if the collection does not contain element.
 * Allows to overcome type-safety restriction of `indexOf` that requires to pass an element of type `T`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Sequence<*>.indexOfRaw(element: Any?): Int {
    return indexOf<Any?>(element)
}

/**
 * Returns the last element.
 * @throws [NoSuchElementException] if the collection is empty.
 */
public fun <T> Sequence<T>.last(): T {
    val iterator = iterator()
    if (!iterator.hasNext())
        throw NoSuchElementException("Sequence is empty.")
    var last = iterator.next()
    while (iterator.hasNext())
        last = iterator.next()
    return last
}

/**
 * Returns the last element matching the given [predicate].
 * @throws [NoSuchElementException] if no such element is found.
 */
public inline fun <T> Sequence<T>.last(predicate: (T) -> Boolean): T {
    var last: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            last = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Collection doesn't contain any element matching the predicate.")
    return last as T
}

/**
 * Returns last index of [element], or -1 if the collection does not contain element.
 */
public fun <T> Sequence<T>.lastIndexOf(element: @kotlin.internal.NoInfer T): Int {
    var lastIndex = -1
    var index = 0
    for (item in this) {
        if (element == item)
            lastIndex = index
        index++
    }
    return lastIndex
}

/**
 * Returns last index of [element], or -1 if the collection does not contain element.
 */
@Deprecated("Use 'indexOfRaw' instead.", ReplaceWith("indexOfRaw(element)"))
@kotlin.jvm.JvmName("lastIndexOfAny")
@kotlin.internal.LowPriorityInOverloadResolution
@Suppress("NOTHING_TO_INLINE")
public fun <T> Sequence<T>.lastIndexOf(element: T): Int {
    return indexOfRaw(element)
}

/**
 * Returns last index of [element], or -1 if the collection does not contain element.
 * Allows to overcome type-safety restriction of `lastIndexOf` that requires to pass an element of type `T`.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Sequence<*>.lastIndexOfRaw(element: Any?): Int {
    return lastIndexOf<Any?>(element)
}

/**
 * Returns the last element, or `null` if the collection is empty.
 */
public fun <T> Sequence<T>.lastOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext())
        return null
    var last = iterator.next()
    while (iterator.hasNext())
        last = iterator.next()
    return last
}

/**
 * Returns the last element matching the given [predicate], or `null` if no such element was found.
 */
public inline fun <T> Sequence<T>.lastOrNull(predicate: (T) -> Boolean): T? {
    var last: T? = null
    for (element in this) {
        if (predicate(element)) {
            last = element
        }
    }
    return last
}

/**
 * Returns the single element, or throws an exception if the collection is empty or has more than one element.
 */
public fun <T> Sequence<T>.single(): T {
    val iterator = iterator()
    if (!iterator.hasNext())
        throw NoSuchElementException("Sequence is empty.")
    var single = iterator.next()
    if (iterator.hasNext())
        throw IllegalArgumentException("Sequence has more than one element.")
    return single
}

/**
 * Returns the single element matching the given [predicate], or throws exception if there is no or more than one matching element.
 */
public inline fun <T> Sequence<T>.single(predicate: (T) -> Boolean): T {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Collection contains more than one matching element.")
            single = element
            found = true
        }
    }
    if (!found) throw NoSuchElementException("Collection doesn't contain any element matching predicate.")
    return single as T
}

/**
 * Returns single element, or `null` if the collection is empty or has more than one element.
 */
public fun <T> Sequence<T>.singleOrNull(): T? {
    val iterator = iterator()
    if (!iterator.hasNext())
        return null
    var single = iterator.next()
    if (iterator.hasNext())
        return null
    return single
}

/**
 * Returns the single element matching the given [predicate], or `null` if element was not found or more than one element was found.
 */
public inline fun <T> Sequence<T>.singleOrNull(predicate: (T) -> Boolean): T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) return null
            single = element
            found = true
        }
    }
    if (!found) return null
    return single
}

/**
 * Returns a sequence containing all elements except first [n] elements.
 */
public fun <T> Sequence<T>.drop(n: Int): Sequence<T> {
    require(n >= 0, { "Requested element count $n is less than zero." })
    return if (n == 0) this else DropSequence(this, n)
}

/**
 * Returns a sequence containing all elements except first elements that satisfy the given [predicate].
 */
public fun <T> Sequence<T>.dropWhile(predicate: (T) -> Boolean): Sequence<T> {
    return DropWhileSequence(this, predicate)
}

/**
 * Returns a sequence containing all elements matching the given [predicate].
 */
public fun <T> Sequence<T>.filter(predicate: (T) -> Boolean): Sequence<T> {
    return FilteringSequence(this, true, predicate)
}

/**
 * Returns a sequence containing all elements not matching the given [predicate].
 */
public fun <T> Sequence<T>.filterNot(predicate: (T) -> Boolean): Sequence<T> {
    return FilteringSequence(this, false, predicate)
}

/**
 * Returns a sequence containing all elements that are not `null`.
 */
public fun <T : Any> Sequence<T?>.filterNotNull(): Sequence<T> {
    return filterNot { it == null } as Sequence<T>
}

/**
 * Appends all elements that are not `null` to the given [destination].
 */
public fun <C : MutableCollection<in T>, T : Any> Sequence<T?>.filterNotNullTo(destination: C): C {
    for (element in this) if (element != null) destination.add(element)
    return destination
}

/**
 * Appends all elements not matching the given [predicate] to the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Sequence<T>.filterNotTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (!predicate(element)) destination.add(element)
    return destination
}

/**
 * Appends all elements matching the given [predicate] into the given [destination].
 */
public inline fun <T, C : MutableCollection<in T>> Sequence<T>.filterTo(destination: C, predicate: (T) -> Boolean): C {
    for (element in this) if (predicate(element)) destination.add(element)
    return destination
}

/**
 * Returns a sequence containing first [n] elements.
 */
public fun <T> Sequence<T>.take(n: Int): Sequence<T> {
    require(n >= 0, { "Requested element count $n is less than zero." })
    return if (n == 0) emptySequence() else TakeSequence(this, n)
}

/**
 * Returns a sequence containing first elements satisfying the given [predicate].
 */
public fun <T> Sequence<T>.takeWhile(predicate: (T) -> Boolean): Sequence<T> {
    return TakeWhileSequence(this, predicate)
}

/**
 * Returns a sequence that yields elements of this sequence sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Sequence<T>.sorted(): Sequence<T> {
    return object : Sequence<T> {
        override fun iterator(): Iterator<T> {
            val sortedList = this@sorted.toArrayList()
            java.util.Collections.sort(sortedList)
            return sortedList.iterator()
        }
    }
}

/**
 * Returns a sequence that yields elements of this sequence sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Sequence<T>.sortedBy(crossinline selector: (T) -> R?): Sequence<T> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a sequence that yields elements of this sequence sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Sequence<T>.sortedByDescending(crossinline selector: (T) -> R?): Sequence<T> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a sequence that yields elements of this sequence sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Sequence<T>.sortedDescending(): Sequence<T> {
    return sortedWith(comparator { x, y -> y.compareTo(x) })
}

/**
 * Returns a sequence that yields elements of this sequence sorted according to the specified [comparator].
 */
public fun <T> Sequence<T>.sortedWith(comparator: Comparator<in T>): Sequence<T> {
    return object : Sequence<T> {
        override fun iterator(): Iterator<T> {
            val sortedList = this@sortedWith.toArrayList()
            java.util.Collections.sort(sortedList, comparator)
            return sortedList.iterator()
        }
    }
}

/**
 * Returns an [ArrayList] of all elements.
 */
public fun <T> Sequence<T>.toArrayList(): ArrayList<T> {
    return toCollection(ArrayList<T>())
}

/**
 * Appends all elements to the given [collection].
 */
public fun <T, C : MutableCollection<in T>> Sequence<T>.toCollection(collection: C): C {
    for (item in this) {
        collection.add(item)
    }
    return collection
}

/**
 * Returns a [HashSet] of all elements.
 */
public fun <T> Sequence<T>.toHashSet(): HashSet<T> {
    return toCollection(HashSet<T>())
}

/**
 * Returns a [LinkedList] containing all elements.
 */
public fun <T> Sequence<T>.toLinkedList(): LinkedList<T> {
    return toCollection(LinkedList<T>())
}

/**
 * Returns a [List] containing all elements.
 */
public fun <T> Sequence<T>.toList(): List<T> {
    return this.toArrayList()
}

@Deprecated("Use toMapBy instead.", ReplaceWith("toMapBy(selector)"))
public inline fun <T, K> Sequence<T>.toMap(selector: (T) -> K): Map<K, T> {
    return toMapBy(selector)
}

/**
 * Returns Map containing the values provided by [transform] and indexed by [selector] from the given collection.
 * If any two elements would have the same key returned by [selector] the last one gets added to the map.
 */
public inline fun <T, K, V> Sequence<T>.toMap(selector: (T) -> K, transform: (T) -> V): Map<K, V> {
    val result = LinkedHashMap<K, V>()
    for (element in this) {
        result.put(selector(element), transform(element))
    }
    return result
}

/**
 * Returns Map containing the values from the given collection indexed by [selector].
 * If any two elements would have the same key returned by [selector] the last one gets added to the map.
 */
public inline fun <T, K> Sequence<T>.toMapBy(selector: (T) -> K): Map<K, T> {
    val result = LinkedHashMap<K, T>()
    for (element in this) {
        result.put(selector(element), element)
    }
    return result
}

/**
 * Returns a [Set] of all elements.
 */
public fun <T> Sequence<T>.toSet(): Set<T> {
    return toCollection(LinkedHashSet<T>())
}

/**
 * Returns a [SortedSet] of all elements.
 */
public fun <T> Sequence<T>.toSortedSet(): SortedSet<T> {
    return toCollection(TreeSet<T>())
}

/**
 * Returns a single sequence of all elements from results of [transform] function being invoked on each element of original sequence.
 */
public fun <T, R> Sequence<T>.flatMap(transform: (T) -> Sequence<R>): Sequence<R> {
    return FlatteningSequence(this, transform)
}

/**
 * Appends all elements yielded from results of [transform] function being invoked on each element of original sequence, to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Sequence<T>.flatMapTo(destination: C, transform: (T) -> Sequence<R>): C {
    for (element in this) {
        val list = transform(element)
        destination.addAll(list)
    }
    return destination
}

/**
 * Returns a map of the elements in original collection grouped by the result of given [toKey] function.
 */
public inline fun <T, K> Sequence<T>.groupBy(toKey: (T) -> K): Map<K, List<T>> {
    return groupByTo(LinkedHashMap<K, MutableList<T>>(), toKey)
}

/**
 * Appends elements from original collection grouped by the result of given [toKey] function to the given [map].
 */
public inline fun <T, K> Sequence<T>.groupByTo(map: MutableMap<K, MutableList<T>>, toKey: (T) -> K): Map<K, MutableList<T>> {
    for (element in this) {
        val key = toKey(element)
        val list = map.getOrPut(key) { ArrayList<T>() }
        list.add(element)
    }
    return map
}

/**
 * Returns a sequence containing the results of applying the given [transform] function to each element of the original sequence.
 */
public fun <T, R> Sequence<T>.map(transform: (T) -> R): Sequence<R> {
    return TransformingSequence(this, transform)
}

/**
 * Returns a sequence containing the results of applying the given [transform] function to each element and its index of the original sequence.
 */
public fun <T, R> Sequence<T>.mapIndexed(transform: (Int, T) -> R): Sequence<R> {
    return TransformingIndexedSequence(this, transform)
}

/**
 * Appends transformed elements and their indices of the original collection using the given [transform] function
 * to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Sequence<T>.mapIndexedTo(destination: C, transform: (Int, T) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}

/**
 * Returns a sequence containing the results of applying the given [transform] function to each non-null element of the original sequence.
 */
@Deprecated("This function will change its semantics soon to map&filter rather than filter&map. Use filterNotNull().map {} instead.", ReplaceWith("filterNotNull().map(transform)"))
public fun <T : Any, R> Sequence<T?>.mapNotNull(transform: (T) -> R): Sequence<R> {
    return TransformingSequence(FilteringSequence(this, false, { it == null }) as Sequence<T>, transform)
}

/**
 * Appends transformed non-null elements of original collection using the given [transform] function
 * to the given [destination].
 */
@Deprecated("This function will change its semantics soon to map&filter rather than filter&map. Use filterNotNull().mapTo(destination) {} instead.", ReplaceWith("filterNotNull().mapTo(destination, transform)"))
public inline fun <T : Any, R, C : MutableCollection<in R>> Sequence<T?>.mapNotNullTo(destination: C, transform: (T) -> R): C {
    for (element in this) {
        if (element != null) {
            destination.add(transform(element))
        }
    }
    return destination
}

/**
 * Appends transformed elements of the original collection using the given [transform] function
 * to the given [destination].
 */
public inline fun <T, R, C : MutableCollection<in R>> Sequence<T>.mapTo(destination: C, transform: (T) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

/**
 * Returns a sequence of [IndexedValue] for each element of the original sequence.
 */
public fun <T> Sequence<T>.withIndex(): Sequence<IndexedValue<T>> {
    return IndexingSequence(this)
}

/**
 * Returns a sequence containing only distinct elements from the given sequence.
 * The elements in the resulting sequence are in the same order as they were in the source sequence.
 */
public fun <T> Sequence<T>.distinct(): Sequence<T> {
    return this.distinctBy { it }
}

/**
 * Returns a sequence containing only distinct elements from the given sequence according to the [keySelector].
 * The elements in the resulting sequence are in the same order as they were in the source sequence.
 */
public fun <T, K> Sequence<T>.distinctBy(keySelector: (T) -> K): Sequence<T> {
    return DistinctSequence(this, keySelector)
}

/**
 * Returns a mutable set containing all distinct elements from the given sequence.
 */
public fun <T> Sequence<T>.toMutableSet(): MutableSet<T> {
    val set = LinkedHashSet<T>()
    for (item in this) set.add(item)
    return set
}

/**
 * Returns `true` if all elements match the given [predicate].
 */
public inline fun <T> Sequence<T>.all(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (!predicate(element)) return false
    return true
}

/**
 * Returns `true` if collection has at least one element.
 */
public fun <T> Sequence<T>.any(): Boolean {
    for (element in this) return true
    return false
}

/**
 * Returns `true` if at least one element matches the given [predicate].
 */
public inline fun <T> Sequence<T>.any(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return true
    return false
}

/**
 * Returns the number of elements in this collection.
 */
public fun <T> Sequence<T>.count(): Int {
    var count = 0
    for (element in this) count++
    return count
}

/**
 * Returns the number of elements matching the given [predicate].
 */
public inline fun <T> Sequence<T>.count(predicate: (T) -> Boolean): Int {
    var count = 0
    for (element in this) if (predicate(element)) count++
    return count
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <T, R> Sequence<T>.fold(initial: R, operation: (R, T) -> R): R {
    var accumulator = initial
    for (element in this) accumulator = operation(accumulator, element)
    return accumulator
}

/**
 * Performs the given [operation] on each element.
 */
public inline fun <T> Sequence<T>.forEach(operation: (T) -> Unit): Unit {
    for (element in this) operation(element)
}

/**
 * Performs the given [operation] on each element, providing sequential index with the element.
 */
public inline fun <T> Sequence<T>.forEachIndexed(operation: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) operation(index++, item)
}

/**
 * Returns the largest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Sequence<T>.max(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (max < e) max = e
    }
    return max
}

/**
 * Returns the first element yielding the largest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>, T : Any> Sequence<T>.maxBy(f: (T) -> R): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var maxElem = iterator.next()
    var maxValue = f(maxElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = f(e)
        if (maxValue < v) {
            maxElem = e
            maxValue = v
        }
    }
    return maxElem
}

/**
 * Returns the smallest element or `null` if there are no elements.
 */
public fun <T : Comparable<T>> Sequence<T>.min(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var min = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (min > e) min = e
    }
    return min
}

/**
 * Returns the first element yielding the smallest value of the given function or `null` if there are no elements.
 */
public inline fun <R : Comparable<R>, T : Any> Sequence<T>.minBy(f: (T) -> R): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var minElem = iterator.next()
    var minValue = f(minElem)
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = f(e)
        if (minValue > v) {
            minElem = e
            minValue = v
        }
    }
    return minElem
}

/**
 * Returns `true` if collection has no elements.
 */
public fun <T> Sequence<T>.none(): Boolean {
    for (element in this) return false
    return true
}

/**
 * Returns `true` if no elements match the given [predicate].
 */
public inline fun <T> Sequence<T>.none(predicate: (T) -> Boolean): Boolean {
    for (element in this) if (predicate(element)) return false
    return true
}

/**
 * Accumulates value starting with the first element and applying [operation] from left to right to current accumulator value and each element.
 */
public inline fun <S, T: S> Sequence<T>.reduce(operation: (S, T) -> S): S {
    val iterator = this.iterator()
    if (!iterator.hasNext()) throw UnsupportedOperationException("Empty iterable can't be reduced.")
    var accumulator: S = iterator.next()
    while (iterator.hasNext()) {
        accumulator = operation(accumulator, iterator.next())
    }
    return accumulator
}

/**
 * Returns the sum of all values produced by [transform] function from elements in the collection.
 */
public inline fun <T> Sequence<T>.sumBy(transform: (T) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += transform(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [transform] function from elements in the collection.
 */
public inline fun <T> Sequence<T>.sumByDouble(transform: (T) -> Double): Double {
    var sum: Double = 0.0
    for (element in this) {
        sum += transform(element)
    }
    return sum
}

/**
 * Returns an original collection containing all the non-`null` elements, throwing an [IllegalArgumentException] if there are any `null` elements.
 */
public fun <T : Any> Sequence<T?>.requireNoNulls(): Sequence<T> {
    return map { it ?: throw IllegalArgumentException("null element found in $this.") }
}

@Deprecated("Use zip() with transform instead.", ReplaceWith("zip(sequence, transform)"))
public fun <T, R, V> Sequence<T>.merge(sequence: Sequence<R>, transform: (T, R) -> V): Sequence<V> {
    return zip(sequence, transform)
}

/**
 * Returns a sequence containing all elements of original sequence except the elements contained in the given [array].
 * Note that the source sequence and the array being subtracted are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.minus(array: Array<out T>): Sequence<T> {
    if (array.isEmpty()) return this
    return object: Sequence<T> {
        override fun iterator(): Iterator<T> {
            val other = array.toHashSet()
            return this@minus.filterNot { it in other }.iterator()
        }
    }
}

/**
 * Returns a sequence containing all elements of original sequence except the elements contained in the given [collection].
 * Note that the source sequence and the collection being subtracted are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.minus(collection: Iterable<T>): Sequence<T> {
    return object: Sequence<T> {
        override fun iterator(): Iterator<T> {
            val other = collection.convertToSetForSetOperation()
            if (other.isEmpty())
                return this@minus.iterator()
            else
                return this@minus.filterNot { it in other }.iterator()
        }
    }
}

/**
 * Returns a sequence containing all elements of the original sequence without the first occurrence of the given [element].
 */
public operator fun <T> Sequence<T>.minus(element: T): Sequence<T> {
    return object: Sequence<T> {
        override fun iterator(): Iterator<T> {
            var removed = false
            return this@minus.filter { if (!removed && it == element) { removed = true; false } else true }.iterator()
        }
    }
}

/**
 * Returns a sequence containing all elements of original sequence except the elements contained in the given [sequence].
 * Note that the source sequence and the sequence being subtracted are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.minus(sequence: Sequence<T>): Sequence<T> {
    return object: Sequence<T> {
        override fun iterator(): Iterator<T> {
            val other = sequence.toHashSet()
            if (other.isEmpty())
                return this@minus.iterator()
            else
                return this@minus.filterNot { it in other }.iterator()
        }
    }
}

/**
 * Splits the original collection into pair of collections,
 * where *first* collection contains elements for which [predicate] yielded `true`,
 * while *second* collection contains elements for which [predicate] yielded `false`.
 */
public inline fun <T> Sequence<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = ArrayList<T>()
    val second = ArrayList<T>()
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first, second)
}

/**
 * Returns a sequence containing all elements of original sequence and then all elements of the given [array].
 * Note that the source sequence and the array being added are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.plus(array: Array<out T>): Sequence<T> {
    return this.plus(array.asList())
}

/**
 * Returns a sequence containing all elements of original sequence and then all elements of the given [collection].
 * Note that the source sequence and the collection being added are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.plus(collection: Iterable<T>): Sequence<T> {
    return sequenceOf(this, collection.asSequence()).flatten()
}

/**
 * Returns a sequence containing all elements of the original sequence and then the given [element].
 */
public operator fun <T> Sequence<T>.plus(element: T): Sequence<T> {
    return sequenceOf(this, sequenceOf(element)).flatten()
}

/**
 * Returns a sequence containing all elements of original sequence and then all elements of the given [sequence].
 * Note that the source sequence and the sequence being added are iterated only when an `iterator` is requested from
 * the resulting sequence. Changing any of them between successive calls to `iterator` may affect the result.
 */
public operator fun <T> Sequence<T>.plus(sequence: Sequence<T>): Sequence<T> {
    return sequenceOf(this, sequence).flatten()
}

/**
 * Returns a sequence of pairs built from elements of both collections with same indexes.
 * Resulting sequence has length of shortest input sequences.
 */
public fun <T, R> Sequence<T>.zip(sequence: Sequence<R>): Sequence<Pair<T, R>> {
    return MergingSequence(this, sequence) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a sequence of values built from elements of both collections with same indexes using provided [transform]. Resulting sequence has length of shortest input sequences.
 */
public fun <T, R, V> Sequence<T>.zip(sequence: Sequence<R>, transform: (T, R) -> V): Sequence<V> {
    return MergingSequence(this, sequence, transform)
}

/**
 * Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T, A : Appendable> Sequence<T>.joinTo(buffer: A, separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...", transform: ((T) -> String)? = null): A {
    buffer.append(prefix)
    var count = 0
    for (element in this) {
        if (++count > 1) buffer.append(separator)
        if (limit < 0 || count <= limit) {
            val text = if (transform != null) transform(element) else if (element == null) "null" else element.toString()
            buffer.append(text)
        } else break
    }
    if (limit >= 0 && count > limit) buffer.append(truncated)
    buffer.append(postfix)
    return buffer
}

/**
 * Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.
 * If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
 * elements will be appended, followed by the [truncated] string (which defaults to "...").
 */
public fun <T> Sequence<T>.joinToString(separator: String = ", ", prefix: String = "", postfix: String = "", limit: Int = -1, truncated: String = "...", transform: ((T) -> String)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
}

/**
 * Returns a sequence from the given collection.
 */
public fun <T> Sequence<T>.asSequence(): Sequence<T> {
    return this
}

/**
 * Returns a sequence containing all elements that are instances of specified type parameter R.
 */
@kotlin.jvm.JvmVersion
public inline fun <reified R> Sequence<*>.filterIsInstance(): Sequence<@kotlin.internal.NoInfer R> {
    return filter { it is R } as Sequence<R>
}

/**
 * Returns a sequence containing all elements that are instances of specified class.
 */
@kotlin.jvm.JvmVersion
public fun <R> Sequence<*>.filterIsInstance(klass: Class<R>): Sequence<R> {
    return filter { klass.isInstance(it) } as Sequence<R>
}

/**
 * Appends all elements that are instances of specified type parameter R to the given [destination].
 */
@kotlin.jvm.JvmVersion
public inline fun <reified R, C : MutableCollection<in R>> Sequence<*>.filterIsInstanceTo(destination: C): C {
    for (element in this) if (element is R) destination.add(element)
    return destination
}

/**
 * Appends all elements that are instances of specified class to the given [destination].
 */
@kotlin.jvm.JvmVersion
public fun <C : MutableCollection<in R>, R> Sequence<*>.filterIsInstanceTo(destination: C, klass: Class<R>): C {
    for (element in this) if (klass.isInstance(element)) destination.add(element as R)
    return destination
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfByte")
public fun Sequence<Byte>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfDouble")
public fun Sequence<Double>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfFloat")
public fun Sequence<Float>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfInt")
public fun Sequence<Int>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfLong")
public fun Sequence<Long>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns an average value of elements in the collection.
 */
@kotlin.jvm.JvmName("averageOfShort")
public fun Sequence<Short>.average(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    var count: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
        count += 1
    }
    return if (count == 0) 0.0 else sum / count
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfByte")
public fun Sequence<Byte>.sum(): Int {
    val iterator = iterator()
    var sum: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfDouble")
public fun Sequence<Double>.sum(): Double {
    val iterator = iterator()
    var sum: Double = 0.0
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfFloat")
public fun Sequence<Float>.sum(): Float {
    val iterator = iterator()
    var sum: Float = 0.0f
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfInt")
public fun Sequence<Int>.sum(): Int {
    val iterator = iterator()
    var sum: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfLong")
public fun Sequence<Long>.sum(): Long {
    val iterator = iterator()
    var sum: Long = 0L
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

/**
 * Returns the sum of all elements in the collection.
 */
@kotlin.jvm.JvmName("sumOfShort")
public fun Sequence<Short>.sum(): Int {
    val iterator = iterator()
    var sum: Int = 0
    while (iterator.hasNext()) {
        sum += iterator.next()
    }
    return sum
}

