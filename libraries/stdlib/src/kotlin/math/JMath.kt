@file:JvmVersion
@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("OperationsKt")
package kotlin.math

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
@kotlin.jvm.JvmName("\$plus")
public operator fun BigInteger.plus(other: BigInteger) : BigInteger = this.add(other)

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
@kotlin.jvm.JvmName("\$minus")
public operator fun BigInteger.minus(other: BigInteger) : BigInteger = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
@kotlin.jvm.JvmName("\$times")
public operator fun BigInteger.times(other: BigInteger) : BigInteger = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigInteger] instances.
 */
@kotlin.jvm.JvmName("\$div")
public operator fun BigInteger.div(other: BigInteger) : BigInteger = this.divide(other)

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
@kotlin.jvm.JvmName("\$unaryMinus")
public operator fun BigInteger.unaryMinus() : BigInteger = this.negate()


/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$plus")
public operator fun BigDecimal.plus(other: BigDecimal) : BigDecimal = this.add(other)

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$minus")
public operator fun BigDecimal.minus(other: BigDecimal) : BigDecimal = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$times")
public operator fun BigDecimal.times(other: BigDecimal) : BigDecimal = this.multiply(other)

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$div")
public operator fun BigDecimal.div(other: BigDecimal) : BigDecimal = this.divide(other)

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$mod")
public operator fun BigDecimal.mod(other: BigDecimal) : BigDecimal = this.remainder(other)

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
@kotlin.jvm.JvmName("\$unaryMinus")
public operator fun BigDecimal.unaryMinus() : BigDecimal = this.negate()
