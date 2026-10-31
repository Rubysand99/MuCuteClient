#!/usr/bin/env bash
# Vá mã nguồn MuCuteRelay để chạy được trên Android: Android không có java.awt.Color
# (codec v800 dùng nó khi đọc PlayerListPacket nên danh sách người chơi bị mất và module combat không có mục tiêu).
# Thay bằng lớp Color tối giản cùng API.
set -euo pipefail

RELAY_DIR="${1:?usage: patch-relay-android.sh <MuCuteRelay dir>}"
CODEC="$RELAY_DIR/Protocol/bedrock-codec/src/main/java/org/cloudburstmc/protocol/bedrock"

mkdir -p "$CODEC/util"
cat > "$CODEC/util/Color.java" <<'JAVA'
package org.cloudburstmc.protocol.bedrock.util;

/** Thay thế tối giản cho java.awt.Color (không có trên Android). Chỉ gồm phần API mà giao thức dùng. */
public final class Color {

    private final int value;

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        this.value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public Color(int argb, boolean hasAlpha) {
        this.value = hasAlpha ? argb : (argb | 0xFF000000);
    }

    public int getRGB() {
        return value;
    }

    public int getAlpha() {
        return (value >>> 24) & 0xFF;
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return value & 0xFF;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Color && ((Color) other).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "Color[argb=0x" + Integer.toHexString(value) + "]";
    }
}
JAVA

# Mọi file dùng java.awt.Color / java.awt.* trong codec trừ lớp Color vừa tạo.
grep -rlE "^import java\.awt\.(Color|\*);" "$CODEC" --include=*.java | while read -r file; do
    sed -i -E 's/^import java\.awt\.(Color|\*);/import org.cloudburstmc.protocol.bedrock.util.Color;/' "$file"
    echo "patched $file"
done

# Kiểm tra: không còn tham chiếu java.awt.Color nào.
if grep -rnE "^import java\.awt\.(Color|\*);|[^a-z]java\.awt\.Color[^a-zA-Z]" "$CODEC" --include=*.java --exclude=Color.java; then
    echo "Con tham chieu java.awt.Color" >&2
    exit 1
fi
