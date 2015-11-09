// WITH_RUNTIME
fun foo() {
    <caret>assert(true, {
        val dummy = 1
        "text"
    })
}