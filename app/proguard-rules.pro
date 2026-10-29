-dontwarn **
-renamesourcefileattribute null
-keep class io.netty.** { *; }
-keep class org.cloudburstmc.netty.** { *; }
-keep class org.cloudburstmc.protocol.bedrock.codec.** { *; }
-keep @io.netty.channel.ChannelHandler$Sharable class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class net.raphimc.minecraftauth.** { *; }
-keep class net.lenni0451.commons.httpclient.** { *; }
-keep class com.mucheng.mucute.client.game.AccountManager { *; }

# jjwt (dùng khi ký yêu cầu đăng nhập Xbox): các lớp được nạp bằng tên qua reflection / ServiceLoader
# nên R8 sẽ xoá hoặc đổi tên nếu không giữ lại, gây "Unable to load class ... StandardSecureDigestAlgorithms".
-keep class io.jsonwebtoken.** { *; }
-keep interface io.jsonwebtoken.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Giữ toàn bộ giao thức Bedrock và relay: gói tin được tạo/giải mã bằng reflection và theo tên enum,
# nếu R8 đổi tên hoặc cắt bớt thì game báo "Server sent broken packet".
-keep class org.cloudburstmc.** { *; }
-keep class com.mucheng.mucute.relay.** { *; }
-keep class it.unimi.dsi.fastutil.** { *; }
-keepclassmembers enum * { *; }
