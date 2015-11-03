class A<T> {
    fun <S> foo(s: S): S = s
    fun <S> bar(<!UNUSED_PARAMETER!>s<!>: S): List<T> = null!!

    fun test() = foo(bar(""))
}
