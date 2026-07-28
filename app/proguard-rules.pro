# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# A 方案：只收缩、不混淆（避免 Retrofit/Gson/OkHttp 因类名重命名而报错）。
# 这是性能优化（减小 dex、降低内存）与网络稳定性之间的折中。
-dontobfuscate

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

# ============================================================
# ML Kit 文本识别（端上中文 OCR）keep 规则 —— 修复 NPE 根因
# ------------------------------------------------------------
# 现象：release 构建（minifyEnabled + shrinkResources，仅收缩不混淆）下，
# 调用 TextRecognition.getClient(ChineseTextRecognizerOptions) 后 process()
# 抛 "getClass() on a null object reference" NPE，OCR 识别失败。
#
# 根因：ML Kit 的 TextRecognizer 实现类与 ComponentRegistrar（TextRegistrar /
# VisionCommonRegistrar / CommonComponentRegistrar）是通过 AndroidManifest.xml
# 的 <meta-data> 字符串 + 运行时 PackageManager 服务发现机制注册的，编译期
# 没有代码引用。R8 在 tree-shaking 阶段认为这些类“未被使用”而删除，运行时
# MlKitComponentDiscoveryService 按清单类名 Class.forName 得到 null，导致
# getClient 返回的 TextRecognizer 内部委托为 null，首次 process() 即对 null
# 调 getClass() 抛 NPE。
#
# 修复：显式 keep 整个 ML Kit 包与 gms 内部识别器实现，禁止 R8 收缩它们。
# 注意：本应用仅收缩不混淆（-dontobfuscate），所以这里用 -keep 防收缩即可。
# ============================================================
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_recognition.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text.** { *; }
# 保住组件发现服务与其读取的注册器（清单元数据反射加载，R8 看不到引用）
-keep class com.google.mlkit.common.internal.MlKitComponentDiscoveryService { *; }
-keep class * extends com.google.mlkit.common.sdkinternal.ComponentRegistrar
# 保留注解/签名/内部类信息，避免反射实例化时因元数据缺失失败
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses, Exceptions
# 静音 ML Kit / gms 内部的缺失类告警（不影响运行）
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_**

# ============================================================
# R8 可选依赖告警静音（非错误，不影响运行）
# 以下类均被 OkHttp 平台探测代码（BouncyCastlePlatform /
# ConscryptPlatform / OpenJSSEPlatform）以反射方式"探测性引用"，
# 但它们是可选的 TLS provider，本应用未打包，运行时不会走到。
# R8 仅告警，不会导致崩溃，这里统一 dontwarn 让构建输出干净。
# 对应的 missing_rules.txt 建议加 -keep，但类根本不在 classpath 中，
# 加 -keep 无效，正确做法是 -dontwarn。
# ============================================================
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
