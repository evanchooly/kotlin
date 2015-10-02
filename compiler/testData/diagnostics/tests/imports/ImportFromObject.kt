// FILE: a.kt
package a

object O {
    class A
    object B

    fun bar() {}
}


object S {

    val prop: String = ""

    fun o(<!UNUSED_PARAMETER!>s<!>: String) = Unit
    fun o(<!UNUSED_PARAMETER!>i<!>: Int) = Unit

    fun Int.ext() = Unit
    var String.ext: Int
        get() = 3
        set(i) {
        }

    fun A(<!UNUSED_PARAMETER!>c<!>: Int) = A()

    class A()
}

// FILE: b.kt
package b

import a.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>O<!>.*

fun testErroneusAllUnderImportFromObject() {
    A()
    B
    <!UNRESOLVED_REFERENCE!>bar<!>()
}

// FILE: c.kt
package c

import a.S.prop
import a.S.o
import a.S.ext
import a.S.A

fun testImportFromObjectByName() {
    prop
    o("a")
    o(3)
    3.ext()
    "".ext = 3
    val <!UNUSED_VARIABLE!>c<!>: Int = "".ext

    A()
    A(3)
}

// FILE: d.kt
package d

import a.S.prop as renamed

fun testFunImportedFromObjectHasNoDispatchReceiver(l: a.S) {
    l.<!UNRESOLVED_REFERENCE!>renamed<!>
    l.prop
    renamed
}