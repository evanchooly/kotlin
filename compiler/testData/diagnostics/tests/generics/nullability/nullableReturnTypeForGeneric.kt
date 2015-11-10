// !DIAGNOSTICS: -UNUSED_PARAMETER
// KT-9893 Incorrect nullability after cast with star projection

open class A

public interface I<T : A> {
    public fun foo(): T?
    public val bar: T?
}

fun acceptA(a: A) {
}

fun main(i: I<*>) {
    acceptA(<!TYPE_MISMATCH!>i.foo()<!>)
    acceptA(<!TYPE_MISMATCH!>i.bar<!>)
}