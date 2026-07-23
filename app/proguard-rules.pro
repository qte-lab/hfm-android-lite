# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Missing classes detected by R8 - dontwarn rules
-dontwarn aQute.bnd.annotation.baseline.BaselineIgnore
-dontwarn aQute.bnd.annotation.spi.ServiceConsumer
-dontwarn aQute.bnd.annotation.spi.ServiceProvider
-dontwarn com.github.luben.zstd.ZstdInputStream
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn java.awt.Color
-dontwarn java.awt.Dimension
-dontwarn java.awt.Rectangle
-dontwarn java.awt.color.ColorSpace
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Dimension2D
-dontwarn java.awt.geom.Path2D
-dontwarn java.awt.geom.PathIterator
-dontwarn java.awt.geom.Point2D
-dontwarn java.awt.geom.Rectangle2D
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ComponentColorModel
-dontwarn java.awt.image.DirectColorModel
-dontwarn java.awt.image.IndexColorModel
-dontwarn java.awt.image.PackedColorModel
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.XMLOutputFactory
-dontwarn javax.xml.stream.XMLEventFactory
-dontwarn javax.xml.stream.XMLReporter
-dontwarn javax.xml.stream.XMLResolver
-dontwarn javax.xml.stream.util.XMLEventAllocator
-dontwarn net.sf.saxon.Configuration
-dontwarn net.sf.saxon.dom.DOMNodeWrapper
-dontwarn net.sf.saxon.om.Item
-dontwarn net.sf.saxon.om.NamespaceUri
-dontwarn net.sf.saxon.om.NodeInfo
-dontwarn net.sf.saxon.om.Sequence
-dontwarn net.sf.saxon.om.SequenceTool
-dontwarn net.sf.saxon.sxpath.IndependentContext
-dontwarn net.sf.saxon.sxpath.XPathDynamicContext
-dontwarn net.sf.saxon.sxpath.XPathEvaluator
-dontwarn net.sf.saxon.sxpath.XPathExpression
-dontwarn net.sf.saxon.sxpath.XPathStaticContext
-dontwarn net.sf.saxon.sxpath.XPathVariable
-dontwarn net.sf.saxon.tree.wrapper.VirtualNode
-dontwarn net.sf.saxon.value.DateTimeValue
-dontwarn net.sf.saxon.value.GDateValue
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference
-dontwarn org.osgi.framework.wiring.BundleRevision
-dontwarn org.tukaani.xz.**

# Retrofit and DTO classes
-keep class com.chronie.homemoneylite.data.remote.dto.** { *; }
-keep class com.chronie.homemoneylite.data.remote.api.** { *; }
-keep class com.chronie.homemoneylite.domain.model.** { *; }

# 显式保留所有“仅通过 Retrofit 泛型返回类型被引用”的响应 DTO。
# 这类类极易被 R8 误判为“未使用”而树摇删除，导致运行时
# Response<SyncResponseDto> 等泛型解析失败（Class cannot be cast to ParameterizedType）。
-keep class com.chronie.homemoneylite.data.remote.dto.SyncResponseDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.ExpenseListResponse { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.ExpenseMetaDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.ExpenseStatisticsDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.TypeDistributionDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.ConflictDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.SyncRequestDto { *; }
-keep class com.chronie.homemoneylite.data.remote.dto.ApiResponse { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# 保留 API 接口及其方法签名（Retrofit 需要泛型返回值信息）
-keep interface com.chronie.homemoneylite.data.remote.api.** { <methods>; }

# Gson
# 保留所有 DTO 类的 @SerializedName 字段
-keepclassmembers class com.chronie.homemoneylite.data.remote.dto.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.chronie.homemoneylite.data.remote.dto.** { <fields>; }
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
# 保留 TypeToken 及其子类的泛型信息（防止 R8 删除 ParameterizedType）
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.* <fields>;
}
