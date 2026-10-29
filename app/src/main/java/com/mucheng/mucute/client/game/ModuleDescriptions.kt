package com.mucheng.mucute.client.game

/**
 * Mô tả ngắn (tiếng Việt) cho từng module, hiện khi mở rộng thẻ module trong menu.
 * Module chưa có mô tả riêng sẽ dùng mô tả chung theo danh mục.
 */
object ModuleDescriptions {

    private val descriptions = mapOf(
        // Combat
        "killaura" to "Tự động tấn công thực thể gần bạn trong tầm 'range'. 'cps' là số đòn mỗi giây (giữ đúng trung bình, tối đa 40), 'max_targets' là số mục tiêu mỗi lượt (sắp gần nhất trước, hoặc ít máu nhất nếu bật 'lowest_health_first'), 'packets' nhân số đòn, 'tp_aura' dịch chuyển tới mục tiêu. Bạn bè (.friend) không bị đánh.",
        "trigger_bot" to "Chỉ tấn công khi có mục tiêu nằm trong tầm 'range' (theo 'cps'), lọc người chơi/quái bằng 2 tuỳ chọn.",
        "hitbox" to "Phóng to vùng trúng đòn (width/height) của thực thể khác để dễ đánh trúng hơn; 'visualize' hiện hạt để bạn thấy vùng đó.",
        "crystal_smash" to "Tự động tấn công thực thể (như pha lê) trong tầm 'range'; 'delay', 'cps', 'packets' chỉnh tốc độ.",
        "hitandrun" to "Tự động đánh rồi nhảy/di chuyển vòng quanh mục tiêu ('circle_radius'), theo tốc độ 'hit_speed'.",
        "anti_knockback" to "Giảm hoặc chặn việc bị đẩy lùi khi bị đánh; 'threshold' là ngưỡng lực đẩy.",
        "anti_crystal" to "Điều chỉnh độ cao ('ylevel') để giảm thiệt hại từ pha lê nổ.",

        // Motion
        "fly" to "Cho phép bay bằng khả năng (ability) của game; tắt module thì trở lại bình thường.",
        "motion_fly" to "Bay bằng cách gửi chuyển động liên tục ('verticalUpSpeed', 'verticalDownSpeed', 'delay'). Dùng khi 'fly' không hoạt động.",
        "no_clip" to "Đi xuyên khối (không va chạm).",
        "sprint" to "Luôn chạy nhanh (sprint) mà không cần giữ nút; tắt thì dừng chạy nhanh.",
        "speed" to "Tăng tốc độ di chuyển.",
        "bhop" to "Nhảy liên tục để tăng tốc độ ('jumpHeight', 'motionInterval', 'times').",
        "high_jump" to "Nhảy cao hơn bình thường ('jumpHeight').",
        "air_jump" to "Cho phép nhảy tiếp khi đang ở trên không.",
        "jetpack" to "Bay lên theo hướng nhìn với tốc độ 'speed' (như bình phản lực).",
        "auto_walk" to "Tự động đi tới phía trước ('walk_speed', 'fall_speed').",
        "anti_afk" to "Tạo chuyển động nhỏ định kỳ ('interval', 'intensity') để tránh bị coi là đứng yên.",

        // Visual
        "esp" to "Vẽ hộp/đường quanh người chơi và thực thể trên màn hình, xuyên tường. Có tuỳ chọn màu theo khoảng cách, cầu vồng, tầm tối đa, tô nền; bạn bè được tô màu riêng.",
        "nametags" to "Hiện bảng tên lớn trên đầu người chơi: tên, khoảng cách, thanh máu (nếu server gửi). Chỉnh màu chữ, độ mờ nền, chế độ chỉ bạn bè.",
        "hud" to "Hiện thông tin trên màn hình: danh sách module đang bật, toạ độ, người chơi gần bạn.",
        "waypoints" to "Vẽ các điểm đánh dấu (.waypoint add <tên>) trên màn hình kèm khoảng cách; lưu riêng theo server, có thể tự tạo điểm khi chết.",
        "breadcrumbs" to "Vẽ vệt đường bạn đã đi để dễ tìm đường quay lại.",
        "coordinates" to "Hiện toạ độ hiện tại của bạn.",
        "free_camera" to "Tách camera khỏi nhân vật để nhìn tự do xung quanh; tắt thì quay lại vị trí cũ.",
        "zoom" to "Phóng to tầm nhìn (như ống ngắm).",
        "no_hurt_camera" to "Tắt hiệu ứng rung/nghiêng màn hình khi bị thương.",
        "speed_display" to "Hiện tốc độ di chuyển hiện tại.",
        "network_info" to "Hiện thông tin mạng, như số gói tin gửi/nhận.",
        "time_shift" to "Đổi thời gian trong ngày (chỉ phía bạn nhìn thấy), chỉnh bằng 'time' (0-24000).",
        "weather_controller" to "Đổi thời tiết (trời quang / mưa / giông) ở phía bạn nhìn thấy.",
        "world_state" to "Hiện thống kê thế giới quanh bạn: số thực thể, người chơi, thời gian, chunk.",

        // Misc
        "command_handler" to "Xử lý lệnh gõ trong khung chat bắt đầu bằng dấu chấm (.help để xem danh sách lệnh). Nên để bật.",
        "baritone" to "Tự động đi tới một điểm đã chọn ('speed', 'jump_height', 'completion_distance').",
        "desync" to "Giữ lại gói chuyển động của bạn khi đang bật, rồi gửi lại từ từ khi tắt. Có thể gây giật vị trí.",
        "fake_death" to "Giả lập hiệu ứng chết/hồi sinh ở phía bạn nhìn thấy.",
        "no_chat" to "Chặn các loại tin nhắn chat (chat người chơi, hệ thống, thì thầm, thông báo, vào/rời) theo tuỳ chọn.",
        "position_logger" to "Ghi lại vị trí của bạn và các thực thể gần đó ('range', 'track_all', 'track_mobs').",
        "replay_mod" to "Ghi lại chuyển động của bạn rồi phát lại; lệnh .replay để điều khiển.",
        "xp_levels" to "Hiện cấp kinh nghiệm giả lập ('levels', 'interval'); chỉ ở phía bạn nhìn thấy.",

        // Effect (riêng lẻ)
        "nightVision" to "Nhìn rõ trong bóng tối."
    )

    private val categoryDescriptions = mapOf(
        ModuleCategory.Effect to "Gửi hiệu ứng trạng thái này cho game của bạn. Hiệu ứng có thể chỉ hiện ở phía bạn và không đổi cách server tính. Có thanh 'amplifier' (nếu có) để chỉnh cấp độ.",
        ModuleCategory.Particle to "Tạo hạt (particle) hiệu ứng hình ảnh quanh bạn.",
    )

    fun of(module: Module): String? {
        return descriptions[module.name] ?: categoryDescriptions[module.category]
    }
}
