package com.ljyh.mei.ui.component.player.component

import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ljyh.mei.constants.Salt3DAngleKey
import com.ljyh.mei.constants.Salt3DBlurMaxKey
import com.ljyh.mei.constants.Salt3DCompensationKey
import com.ljyh.mei.constants.Salt3DDepthKey
import com.ljyh.mei.constants.Salt3DDurationKey
import com.ljyh.mei.constants.Salt3DFontWeightKey
import com.ljyh.mei.constants.Salt3DGapKey
import com.ljyh.mei.constants.Salt3DGuideKey
import com.ljyh.mei.constants.Salt3DLetterSpacingKey
import com.ljyh.mei.constants.Salt3DPadKey
import com.ljyh.mei.constants.Salt3DPerspectiveKey
import com.ljyh.mei.constants.Salt3DShowQuickPanelKey
import com.ljyh.mei.constants.Salt3DTraditionalKey
import com.ljyh.mei.utils.rememberPreference
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeSyllable
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 缓出三次方曲线 (1 - (1 - t)^3)，与参考 HTML 中的 `1 - Math.pow(1 - t, 3)` 完全一致，
 * 提供带有柔和惯性的空间滚动体验。
 */
private val SaltEaseOutCubic = Easing { t ->
    1f - (1f - t).pow(3)
}

/**
 * 离线简繁转换字典（与参考 HTML 方案保持一致，支持词组优先与单字兜底）
 */
private object SaltTraditionalConverter {
    private const val S2T_SRC =
        "页頁还還讯訊号號飞飛晓曉发發颗顆卫衛场場给給爱愛个個脏臟温溫尽盡头頭会會这這轻輕" +
            "边邊于於内內书書后後长長门門问問间間闻聞们們来來时時实實说說过過对對无無与與为為" +
            "从從众眾见見贝貝车車电電风風云雲气氣点點马馬鸟鳥龙龍岁歲归歸当當应應开開关關" +
            "兴興学學觉覺记記认認識識话話语語请請读讀谁誰难難虽雖随隨际際阳陽阴陰队隊阶階陆陸" +
            "陈陳邻鄰邮郵块塊坚堅坏壞坟墳墙牆声聲备備复復够夠夺奪奋奮奖獎妇婦妈媽娇嬌孙孫宁寧" +
            "宾賓宝寶审審宽寬导導寿壽将將尔爾尘塵尝嘗层層属屬岭嶺岛島峡峽帅帥师師帐帳帮幫带帶" +
            "录錄忆憶忧憂怀懷态態总總恋戀惊驚愿願惯慣懒懶戏戲战戰担擔挤擠摆擺摄攝拥擁择擇换換" +
            "损損摇搖摊攤据據抢搶护護报報拨撥挂掛挡擋挥揮捞撈捡撿扫掃扬揚执執扩擴扰擾抚撫递遞" +
            "适適选選迁遷远遠进進运運达達违違连連迟遲逻邏郑鄭观觀广廣写寫乐樂欢歡环環获獲" +
            "击擊鸡雞积積极極几幾继繼价價艰艱讲講胶膠节節结結仅僅旧舊举舉克剋亏虧阔闊" +
            "腊臘蜡蠟蓝藍劳勞类類泪淚礼禮历歷丽麗联聯脸臉练練粮糧两兩辆輛疗療临臨灵靈龄齡楼樓乱亂" +
            "罗羅吗嗎买買卖賣麦麥么麼梦夢弥彌庙廟灭滅鸣鳴铭銘亩畝脑腦闹鬧拟擬农農欧歐盘盤庞龐" +
            "赔賠骗騙频頻凭憑评評扑撲朴樸启啟牵牽纤纖浅淺枪槍乔喬桥橋亲親倾傾庆慶穷窮区區权權劝勸确確" +
            "让讓热熱荣榮软軟洒灑伞傘杀殺伤傷设設胜勝湿濕诗詩势勢释釋试試视視兽獸术術树樹双雙顺順" +
            "松鬆苏蘇诉訴缩縮叹嘆汤湯讨討腾騰体體条條贴貼铁鐵听聽厅廳图圖团團围圍维維伟偉务務雾霧误誤" +
            "习習细細虾蝦吓嚇显顯险險现現线線乡鄉详詳响響项項销銷协協须須许許寻尋压壓亚亞严嚴盐鹽颜顏验驗" +
            "养養样樣药藥爷爺业業叶葉医醫仪儀亿億义義议議艺藝饮飲隐隱营營优優犹猶游遊余餘鱼魚誉譽园園约約跃躍" +
            "杂雜载載赞讚责責择擇泽澤张張赵趙征徵证證织織职職纸紙质質钟鐘种種周週专專转轉状狀庄莊装裝准準浊濁资資踪蹤纵縱钻鑽"

    private val charMap: Map<Char, Char> by lazy {
        val map = HashMap<Char, Char>(S2T_SRC.length / 2)
        var i = 0
        while (i + 1 < S2T_SRC.length) {
            map[S2T_SRC[i]] = S2T_SRC[i + 1]
            i += 2
        }
        map
    }

    private val phrases: List<Pair<String, String>> by lazy {
        listOf(
            "千钧一发" to "千鈞一髮", "一见钟情" to "一見鍾情", "头发" to "頭髮", "理发" to "理髮",
            "白发" to "白髮", "发丝" to "髮絲", "发廊" to "髮廊", "肮脏" to "骯髒",
            "复杂" to "複雜", "重复" to "重複", "复习" to "複習", "日历" to "日曆", "历法" to "曆法",
            "钟爱" to "鍾愛", "长征" to "長征", "征服" to "征服", "周围" to "周圍", "四周" to "四周",
            "批准" to "批准", "标准" to "標準", "松树" to "松樹", "放松" to "放鬆", "游泳" to "游泳",
            "里面" to "裡面", "心里" to "心裡", "那里" to "那裡", "这里" to "這裡", "哪里" to "哪裡",
            "夜里" to "夜裡", "梦里" to "夢裡", "公里" to "公里", "故里" to "故里", "皇后" to "皇后",
            "天后" to "天后", "身后" to "身後", "背后" to "背後", "以后" to "以後", "最后" to "最後",
            "干杯" to "乾杯", "干净" to "乾淨", "干活" to "幹活", "树干" to "樹幹", "茶几" to "茶几",
            "尽管" to "儘管", "面包" to "麵包", "面条" to "麵條", "面前" to "面前", "画面" to "畫面",
            "日子里" to "日子裏", "给我" to "給我", "再给" to "再給", "一瓶酒" to "一瓶酒", "一支烟" to "一支煙",
            "说走就走" to "說走就走", "我有的是时间" to "我有的是時間", "独自笑着" to "獨自笑着", "无法往前" to "無法往前",
        ).sortedByDescending { it.first.length }
    }

    fun convert(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            var matched: Pair<String, String>? = null
            for (p in phrases) {
                if (input.startsWith(p.first, i)) {
                    matched = p
                    break
                }
            }
            if (matched != null) {
                sb.append(matched.second)
                i += matched.first.length
            } else {
                val c = input[i]
                sb.append(charMap[c] ?: c)
                i++
            }
        }
        return sb.toString()
    }
}

private data class SaltDisplayLine(
    val originalIndex: Int,
    val line: ISyncedLine,
    val primaryText: String,
    val translationText: String?,
    val phoneticText: String?,
)

private data class SaltMeasuredGeometry(
    val tops: FloatArray,
    val heights: FloatArray,
    val avgStepPx: Float,
)

/**
 * Salt Player 风格 3D 立体空间歌词视图（左对齐景深方案）
 *
 * 严格遵循三条硬约束：
 * 1. 整列不旋转、不居中：视口容器提供透视，每行统一左缩进 `left = pad`。
 * 2. 每行单独绕自身竖直轴向内旋转 `rotateY(-angle)`，以左端为旋转与缩放基准 -> 左退右近，形成立体书页空间。
 * 3. 左对齐采用解析几何补偿 `dx = (vx - x0) * z / P`，确保任意深度与景深缩放下所有行起点严格落在同一竖线。
 */
@Composable
internal fun Salt3DLyricsView(
    lyrics: SyncedLyrics,
    currentPosition: () -> Int,
    isPlaying: Boolean,
    showTranslation: Boolean,
    showPhonetic: Boolean,
    mainFontSizeSp: Float,
    translationFontSizeSp: Float,
    onLineClicked: (ISyncedLine) -> Unit,
    onLinePressed: (ISyncedLine) -> Unit,
    controlsVisible: Boolean,
    onToggleControls: (Boolean) -> Unit,
    topFadeHeight: Dp = 20.dp,
    bottomFadeHeight: Dp = 100.dp,
    controlsFadeRange: (Float) -> ClosedFloatingPointRange<Float>? = { null },
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()

    // 持久化 3D 参数配置（默认值严格对齐参考 HTML 方案）
    val (angleDeg, setAngleDeg) = rememberPreference(Salt3DAngleKey, 40f)
    val (depthVal, setDepthVal) = rememberPreference(Salt3DDepthKey, 60f)
    val (perspectiveP, setPerspectiveP) = rememberPreference(Salt3DPerspectiveKey, 900f)
    val (durationMs, setDurationMs) = rememberPreference(Salt3DDurationKey, 440)
    val (blurMaxDp, setBlurMaxDp) = rememberPreference(Salt3DBlurMaxKey, 5.5f)
    val (baseWeight, setBaseWeight) = rememberPreference(Salt3DFontWeightKey, 600)
    val (gapDp, setGapDp) = rememberPreference(Salt3DGapKey, 24f)
    val (letterSpacingPx, setLetterSpacingPx) = rememberPreference(Salt3DLetterSpacingKey, 0f)
    val (customPadDp, setCustomPadDp) = rememberPreference(Salt3DPadKey, -1f)
    val (traditional, setTraditional) = rememberPreference(Salt3DTraditionalKey, false)
    val (useComp, setUseComp) = rememberPreference(Salt3DCompensationKey, true)
    val (showGuide, setShowGuide) = rememberPreference(Salt3DGuideKey, false)
    val (showQuickPanel, _) = rememberPreference(Salt3DShowQuickPanelKey, true)

    var panelExpanded by remember { mutableStateOf(false) }

    // 过滤出主歌词行（排除伴唱独立子行，避免空高干扰距离计算）
    val accompanimentOwners = remember(lyrics.lines) {
        lyricAccompanimentOwners(lyrics.lines)
    }

    val displayLines = remember(lyrics.lines, traditional, showTranslation, showPhonetic) {
        val result = ArrayList<SaltDisplayLine>(lyrics.lines.size)
        lyrics.lines.forEachIndexed { idx, line ->
            if (line is KaraokeLine.AccompanimentKaraokeLine) return@forEachIndexed
            val rawMain = when (line) {
                is KaraokeLine -> line.syllables.joinToString("") { it.content }
                is SyncedLine -> line.content
                else -> ""
            }
            val rawTrans = when (line) {
                is KaraokeLine -> line.translation
                is SyncedLine -> line.translation
                else -> null
            }?.takeIf { showTranslation && it.isNotBlank() }

            val rawPhonetic = when (line) {
                is KaraokeLine -> line.phonetic
                    ?: line.syllables.mapNotNull(KaraokeSyllable::phonetic)
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(" ")
                else -> null
            }?.takeIf { showPhonetic && it.isNotBlank() }

            val mainText = if (traditional) SaltTraditionalConverter.convert(rawMain) else rawMain
            val transText = rawTrans?.let {
                if (traditional) SaltTraditionalConverter.convert(it) else it
            }
            result.add(
                SaltDisplayLine(
                    originalIndex = idx,
                    line = line,
                    primaryText = mainText.ifBlank { "···" },
                    translationText = transText,
                    phoneticText = rawPhonetic,
                )
            )
        }
        result
    }

    val lineCount = displayLines.size
    if (lineCount == 0) return

    // 原始行索引 -> 显示行索引映射
    val originalToDisplayMap = remember(displayLines, lyrics.lines.size) {
        val map = IntArray(lyrics.lines.size) { 0 }
        var currentDisp = 0
        for (i in lyrics.lines.indices) {
            while (
                currentDisp + 1 < displayLines.size &&
                displayLines[currentDisp + 1].originalIndex <= i
            ) {
                currentDisp++
            }
            map[i] = currentDisp
        }
        map
    }

    val targetDisplayIndex by remember(lyrics.lines, accompanimentOwners, originalToDisplayMap) {
        derivedStateOf {
            val posMs = currentPosition()
            val rawFocus = lyricFocusLineIndex(lyrics.lines, posMs, accompanimentOwners)
                .coerceIn(0, (lyrics.lines.size - 1).coerceAtLeast(0))
            originalToDisplayMap.getOrElse(rawFocus) { 0 }
                .coerceIn(0, (lineCount - 1).coerceAtLeast(0))
        }
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val widthPx = viewportSize.width.toFloat().coerceAtLeast(1f)
    val heightPx = viewportSize.height.toFloat().coerceAtLeast(1f)
    val viewportWidthDp = with(density) { widthPx.toDp().value }

    // 自适应左缩进：窄屏 26dp，宽屏 clamp(28dp, 7vw, 92dp)，或用户手动滑块值
    val adaptivePadDp = remember(viewportWidthDp) {
        if (viewportWidthDp <= 560f) 26f
        else (viewportWidthDp * 0.07f).coerceIn(28f, 92f)
    }
    val effectivePadDp = if (customPadDp >= 0f) customPadDp else adaptivePadDp
    val padPx = with(density) { effectivePadDp.dp.toPx() }

    // 每行最大排版宽度：手机屏 max(74vw)，桌面/平板 min(340dp..440dp, 62vw)
    val maxRowWidthPx = remember(widthPx, padPx, viewportWidthDp, density) {
        val maxByRatio = if (viewportWidthDp <= 560f) widthPx * 0.74f else widthPx * 0.62f
        val maxByDp = with(density) {
            if (viewportWidthDp <= 560f) 340.dp.toPx() else 420.dp.toPx()
        }
        val available = (widthPx - padPx - with(density) { 20.dp.toPx() }).coerceAtLeast(120f)
        minOf(maxByRatio, maxByDp, available).roundToInt().coerceAtLeast(100)
    }

    val effectiveFontSizeSp = mainFontSizeSp.coerceIn(18f, 34f)
    val effectiveTransSizeSp = translationFontSizeSp.coerceIn(13f, 22f)
    val effectivePhoneticSizeSp = (effectiveFontSizeSp * 0.52f).coerceIn(11f, 16f)

    // 测量前统一按最粗字重 (FontWeight.Black = 900) + 当前字距排版，确保滚动与字重过渡时长句换行位置绝不跳动
    val geometry = remember(
        displayLines,
        maxRowWidthPx,
        effectiveFontSizeSp,
        effectiveTransSizeSp,
        effectivePhoneticSizeSp,
        letterSpacingPx,
        gapDp,
        density,
        textMeasurer,
    ) {
        val gapPx = with(density) { gapDp.dp.toPx() }
        val subGapPx = with(density) { 4.dp.toPx() }
        val measureMainStyle = TextStyle(
            fontSize = effectiveFontSizeSp.sp,
            lineHeight = 1.5.em,
            fontWeight = FontWeight.Black,
            letterSpacing = letterSpacingPx.sp,
        )
        val measureTransStyle = TextStyle(
            fontSize = effectiveTransSizeSp.sp,
            lineHeight = 1.4.em,
            fontWeight = FontWeight.Bold,
            letterSpacing = (letterSpacingPx * 0.5f).sp,
        )
        val measurePhoneticStyle = TextStyle(
            fontSize = effectivePhoneticSizeSp.sp,
            lineHeight = 1.35.em,
            fontWeight = FontWeight.Medium,
        )
        val constraints = Constraints(maxWidth = maxRowWidthPx)
        val tops = FloatArray(lineCount)
        val heights = FloatArray(lineCount)
        var cursorY = 0f
        for (i in 0 until lineCount) {
            val item = displayLines[i]
            val mainLayout = textMeasurer.measure(
                text = item.primaryText,
                style = measureMainStyle,
                constraints = constraints,
            )
            var h = mainLayout.size.height.toFloat()
            if (item.phoneticText != null) {
                val phoLayout = textMeasurer.measure(
                    text = item.phoneticText,
                    style = measurePhoneticStyle,
                    constraints = constraints,
                )
                h += subGapPx + phoLayout.size.height
            }
            if (item.translationText != null) {
                val tranLayout = textMeasurer.measure(
                    text = item.translationText,
                    style = measureTransStyle,
                    constraints = constraints,
                )
                h += subGapPx + tranLayout.size.height
            }
            tops[i] = cursorY
            heights[i] = h.coerceAtLeast(with(density) { 28.dp.toPx() })
            cursorY += heights[i] + gapPx
        }
        val avgStep = if (lineCount > 1) {
            (tops[lineCount - 1] - tops[0]) / (lineCount - 1).toFloat()
        } else {
            (heights.firstOrNull() ?: 60f) + gapPx
        }
        SaltMeasuredGeometry(
            tops = tops,
            heights = heights,
            avgStepPx = avgStep.coerceAtLeast(36f),
        )
    }

    // 连续浮点位置 pos（驱动 y / z / dx / scale / blur / opacity / weight 同步连续变化）
    val posAnim = remember(lyrics) {
        Animatable(targetDisplayIndex.toFloat())
    }
    var isManualBrowsing by remember(lyrics) { mutableStateOf(false) }
    var lastManualTouchRealtime by remember(lyrics) { mutableLongStateOf(0L) }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    // 自动跟随播放行：440ms easeOutCubic 平滑空间运动
    LaunchedEffect(targetDisplayIndex, isManualBrowsing, durationMs) {
        if (!isManualBrowsing) {
            val target = targetDisplayIndex.toFloat()
            val distance = abs(target - posAnim.value)
            if (distance > 12f) {
                // 跨大段 Seek 时直接快速定位
                posAnim.snapTo(target)
            } else if (distance > 0.001f) {
                posAnim.animateTo(
                    targetValue = target,
                    animationSpec = tween(
                        durationMillis = durationMs.coerceIn(150, 1200),
                        easing = SaltEaseOutCubic,
                    ),
                )
            }
        }
    }

    // 手动浏览超时 3.2 秒后自动平滑归位
    LaunchedEffect(isManualBrowsing, lastManualTouchRealtime) {
        if (isManualBrowsing) {
            delay(3_200L)
            if (SystemClock.elapsedRealtime() - lastManualTouchRealtime >= 3_100L) {
                isManualBrowsing = false
            }
        }
    }

    fun jumpToDisplayIndex(index: Int, seekAudio: Boolean = false) {
        val clamped = index.coerceIn(0, lineCount - 1)
        flingJob?.cancel()
        isManualBrowsing = false
        if (seekAudio) {
            onLineClicked(displayLines[clamped].line)
        }
        scope.launch {
            posAnim.animateTo(
                targetValue = clamped.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMs.coerceIn(150, 1200),
                    easing = SaltEaseOutCubic,
                ),
            )
        }
    }

    // topAt(p) 连续插值函数（与参考 HTML 完全一致）
    fun topAt(p: Float): Float {
        val clampedP = p.coerceIn(0f, (lineCount - 1).coerceAtLeast(0).toFloat())
        val i = floor(clampedP).toInt().coerceIn(0, lineCount - 1)
        val f = clampedP - i
        val a = geometry.tops[i]
        val b = if (i + 1 < lineCount) {
            geometry.tops[i + 1]
        } else {
            a + geometry.avgStepPx
        }
        return a + (b - a) * f
    }

    // focusH(p) 连续插值，消除 round(pos) 在 0.5 处的微小跳变
    fun heightAt(p: Float): Float {
        val clampedP = p.coerceIn(0f, (lineCount - 1).coerceAtLeast(0).toFloat())
        val i = floor(clampedP).toInt().coerceIn(0, lineCount - 1)
        val f = clampedP - i
        val a = geometry.heights[i]
        val b = if (i + 1 < lineCount) geometry.heights[i + 1] else a
        return a + (b - a) * f
    }

    // 缓存量化后的高斯模糊 RenderEffect（0.25dp 步进），避免滚动期间每帧重建 RenderEffect 与 Modifier 节点
    val blurEffectCache = remember(density) {
        HashMap<Int, BlurEffect>(48)
    }

    val currentPos = posAnim.value
    val focusY = heightPx * 0.44f
    val focusH = heightAt(currentPos)
    val base = focusY - topAt(currentPos) - focusH * 0.5f
    val vx = widthPx * 0.5f
    val k = depthVal / 60f
    val densityVal = density.density
    val zNearPx = 18f * densityVal * k
    val zStepPx = 26f * densityVal * k
    val shrink = 0.045f * k
    val dim = 0.05f * k
    val fadeK = 0.4f + 0.6f * k
    val pPx = (perspectiveP * densityVal).coerceAtLeast(300f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(lineCount, geometry, durationMs, controlsVisible) {
                val touchSlop = viewConfiguration.touchSlop
                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Main,
                    )
                    down.consume()
                    flingJob?.cancel()
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    val downTime = SystemClock.elapsedRealtime()
                    var dragged = false
                    var longPressTriggered = false
                    var pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == pointerId }
                            ?: event.changes.firstOrNull()
                            ?: break
                        pointerId = change.id
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        val totalDelta = change.position - down.position
                        if (!dragged && totalDelta.getDistance() > touchSlop) {
                            dragged = true
                            isManualBrowsing = true
                            lastManualTouchRealtime = SystemClock.elapsedRealtime()
                            if (totalDelta.y < -10f) {
                                onToggleControls(false)
                            } else if (totalDelta.y > 10f) {
                                onToggleControls(true)
                            }
                        }

                        if (
                            !dragged &&
                            !longPressTriggered &&
                            SystemClock.elapsedRealtime() - downTime >= longPressTimeout
                        ) {
                            longPressTriggered = true
                            val hitIdx = findHitRowIndex(
                                touchY = down.position.y,
                                currentPos = posAnim.value,
                                focusY = heightPx * 0.44f,
                                base = focusY - topAt(posAnim.value) - heightAt(posAnim.value) * 0.5f,
                                pPx = pPx,
                                zNearPx = zNearPx,
                                zStepPx = zStepPx,
                                shrink = shrink,
                                geometry = geometry,
                            )
                            if (hitIdx in 0 until lineCount) {
                                onLinePressed(displayLines[hitIdx].line)
                            }
                        }

                        if (dragged) {
                            val dy = change.positionChange().y
                            if (dy != 0f) {
                                change.consume()
                                lastManualTouchRealtime = SystemClock.elapsedRealtime()
                                val deltaPos = -dy / geometry.avgStepPx
                                val nextPos = (posAnim.value + deltaPos).coerceIn(
                                    0f,
                                    (lineCount - 1).coerceAtLeast(0).toFloat()
                                )
                                scope.launch {
                                    posAnim.snapTo(nextPos)
                                }
                            }
                        }

                        if (!change.pressed) {
                            change.consume()
                            if (dragged) {
                                val velocityY = velocityTracker.calculateVelocity().y
                                val velocityPos = -velocityY / geometry.avgStepPx
                                if (abs(velocityPos) > 0.4f) {
                                    flingJob = scope.launch {
                                        posAnim.updateBounds(
                                            0f,
                                            (lineCount - 1).coerceAtLeast(0).toFloat()
                                        )
                                        try {
                                            posAnim.animateDecay(
                                                initialVelocity = velocityPos,
                                                animationSpec = exponentialDecay(frictionMultiplier = 1.8f),
                                            )
                                        } finally {
                                            lastManualTouchRealtime = SystemClock.elapsedRealtime()
                                        }
                                    }
                                }
                            } else if (!longPressTriggered) {
                                val hitIdx = findHitRowIndex(
                                    touchY = change.position.y,
                                    currentPos = posAnim.value,
                                    focusY = heightPx * 0.44f,
                                    base = focusY - topAt(posAnim.value) - heightAt(posAnim.value) * 0.5f,
                                    pPx = pPx,
                                    zNearPx = zNearPx,
                                    zStepPx = zStepPx,
                                    shrink = shrink,
                                    geometry = geometry,
                                )
                                if (
                                    hitIdx in 0 until lineCount &&
                                    change.position.x >= padPx - with(density) { 16.dp.toPx() } &&
                                    change.position.x <= padPx + maxRowWidthPx + with(density) { 24.dp.toPx() }
                                ) {
                                    jumpToDisplayIndex(hitIdx, seekAudio = true)
                                } else {
                                    onToggleControls(!controlsVisible)
                                }
                            }
                            break
                        }
                    }
                }
            }
    ) {
        // 1. 远景氛围暗角与柔雾层（使用 drawWithCache 缓存径向渐变 Shader，消除无操作时的无限重绘）
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val w = size.width.coerceAtLeast(1f)
                    val h = size.height.coerceAtLeast(1f)
                    val veilCenter = Offset(w * 0.20f, h * 0.44f)
                    val veilRadius = max(w, h) * 0.55f
                    val veilBrush = Brush.radialGradient(
                        colors = listOf(Color(0x4D04080E), Color.Transparent),
                        center = veilCenter,
                        radius = veilRadius,
                    )
                    val fogCenter = Offset(w * 0.26f, h * 0.44f)
                    val fogRadius = max(w, h) * 0.445f
                    val fogBrush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.045f), Color.Transparent),
                        center = fogCenter,
                        radius = fogRadius,
                    )
                    onDrawBehind {
                        drawCircle(
                            brush = veilBrush,
                            center = veilCenter,
                            radius = veilRadius,
                        )
                        drawCircle(
                            brush = fogBrush,
                            center = fogCenter,
                            radius = fogRadius,
                        )
                    }
                }
        )

        // 2. 3D 歌词舞台（顶部 0%~11% 与底部 75%~97% 渐隐遮罩）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithCache {
                    val h = size.height.coerceAtLeast(1f)
                    val topStop = max(0.11f, topFadeHeight.toPx() / h).coerceIn(0.05f, 0.25f)
                    val bottomStop = min(0.75f, 1f - bottomFadeHeight.toPx() / h).coerceIn(0.55f, 0.88f)
                    val stageFadeBrush = Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        topStop to Color.Black,
                        bottomStop to Color.Black,
                        0.97f to Color.Transparent,
                        1.00f to Color.Transparent,
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = stageFadeBrush,
                            blendMode = BlendMode.DstIn,
                        )
                        controlsFadeRange(h)?.let { range ->
                            if (range.start < h && range.endInclusive > range.start) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = range.start,
                                        endY = range.endInclusive,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                    }
                }
        ) {
            // 仅渲染可视景深窗口内的歌词行（|i - pos| <= 7.5，超过 7 的行透明度为 0）
            val startIdx = (floor(currentPos) - 7).toInt().coerceAtLeast(0)
            val endIdx = (floor(currentPos) + 8).toInt().coerceAtMost(lineCount - 1)

            for (i in startIdx..endIdx) {
                val item = displayLines[i]
                val d = i.toFloat() - currentPos
                val ad = abs(d)
                if (ad > 7.2f) continue

                key(i) {
                    val near = (1f - ad).coerceAtLeast(0f)
                    val z = zNearPx * near - zStepPx * ad
                    val rowTopY = base + geometry.tops[i]
                    val rowH = geometry.heights[i].coerceAtLeast(1f)
                    val yCenter = rowTopY + rowH * 0.5f

                    // 空间缩放与透视投影比例
                    val scale = (1f - shrink * ad).coerceAtLeast(0.55f)
                    val perspZ = (pPx / (pPx - z).coerceAtLeast(120f)).coerceIn(0.35f, 1.35f)
                    val effectiveScale = scale * perspZ

                    // 解析左对齐补偿：dx = (vx - pad) * z / P，启用时所有行起点严格落在 x = pad 竖线上；
                    // 关闭时重现远处行向中心消失点 vx 漂移的现象。
                    val screenLeftX = if (useComp) {
                        padPx
                    } else {
                        vx + (padPx - vx) * perspZ
                    }

                    // 共享 44% 视觉地平线 (focusY) 的 3D 枢轴坐标转换：
                    // 使行上方呈仰视斜率 (/)、焦点行水平 (—)、行下方呈俯视斜率 (\)，完全复刻 CSS perspective-origin: 50% 44%
                    val pivotY = rowH * 0.5f - (yCenter - focusY) / scale
                    val pivotFractionY = (pivotY / rowH).coerceIn(-40f, 40f)
                    val translationY = focusY - pivotY
                    val effectiveCameraDistance = ((pPx - z).coerceAtLeast(120f) / 72f).coerceIn(4f, 60f)

                    // 景深模糊、指数衰减透明度与空气透视亮度
                    val blurVal = min(blurMaxDp, 1.35f * k * ad)
                    val blurStep = (blurVal * 4f).roundToInt()
                    val rowBlurEffect = if (blurStep > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        blurEffectCache.getOrPut(blurStep) {
                            val radiusPx = (blurStep * 0.25f) * densityVal
                            BlurEffect(radiusPx, radiusPx, TileMode.Decal)
                        }
                    } else null

                    val fadeRate = if (d < 0f) 0.55f else 0.40f
                    val fade = exp(-ad * fadeRate * fadeK)
                    val opacity = if (isManualBrowsing) {
                        max(fade, if (ad < 3.5f) 0.48f else 0.22f).coerceIn(0f, 1f)
                    } else {
                        fade.coerceIn(0f, 1f)
                    }
                    val bright = (1f - dim * ad).coerceAtLeast(0.55f)
                    val textAlpha = (0.78f + 0.22f * near).coerceIn(0f, 1f)
                    val mainColor = Color(
                        red = bright,
                        green = bright,
                        blue = bright,
                        alpha = textAlpha,
                    )

                    // 字重量化为 50 步进（600/650/700/750/800），命中 Skia 字形缓存，避免滚动时每帧重塑可变字体
                    val rowWeight = min(
                        900,
                        (((baseWeight + 200f * near) / 50f).roundToInt() * 50).coerceAtLeast(400)
                    )
                    // 焦点行极细白色描边增重（对应 HTML 中的 webkitTextStroke，无阴影无霓虹）
                    val quantizedNear = (near * 10f).roundToInt() / 10f
                    val strokeWidthPx =
                        ((0.2f + 0.8f * quantizedNear) * max(0f, (baseWeight - 400f) / 500f) * 0.9f + 0.35f * quantizedNear) * densityVal

                    // 外层 .row：仅承载纯粹的 3D 变换 (translate3d + rotateY + scale)，保证 GPU 3D 投影无裁剪
                    Box(
                        modifier = Modifier
                            .width(with(density) { maxRowWidthPx.toDp() })
                            .heightIn(min = with(density) { rowH.toDp() })
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, pivotFractionY)
                                translationX = screenLeftX
                                this.translationY = translationY
                                scaleX = effectiveScale
                                scaleY = effectiveScale
                                rotationY = -angleDeg
                                cameraDistance = effectiveCameraDistance
                            }
                    ) {
                        // 内层 .ink：在 RenderNode 层直接执行 2D 高斯模糊 (renderEffect) 与透明度衰减，零 Modifier 重建开销
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = opacity
                                    renderEffect = rowBlurEffect
                                },
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val rowTextStyle = TextStyle(
                                fontSize = effectiveFontSizeSp.sp,
                                lineHeight = 1.5.em,
                                fontWeight = FontWeight(rowWeight),
                                letterSpacing = letterSpacingPx.sp,
                                textAlign = TextAlign.Start,
                            )

                            Box {
                                if (strokeWidthPx > 0.15f && quantizedNear > 0.05f) {
                                    Text(
                                        text = item.primaryText,
                                        style = rowTextStyle.copy(
                                            drawStyle = Stroke(width = strokeWidthPx),
                                        ),
                                        color = Color(
                                            red = bright,
                                            green = bright,
                                            blue = bright,
                                            alpha = (0.95f * near).coerceIn(0f, 1f),
                                        ),
                                    )
                                }
                                Text(
                                    text = item.primaryText,
                                    style = rowTextStyle,
                                    color = mainColor,
                                )
                            }

                            if (item.phoneticText != null) {
                                Text(
                                    text = item.phoneticText,
                                    style = TextStyle(
                                        fontSize = effectivePhoneticSizeSp.sp,
                                        lineHeight = 1.35.em,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Start,
                                    ),
                                    color = Color(
                                        red = bright,
                                        green = bright,
                                        blue = bright,
                                        alpha = (0.58f + 0.18f * near).coerceIn(0f, 1f),
                                    ),
                                )
                            }

                            if (item.translationText != null) {
                                Text(
                                    text = item.translationText,
                                    style = TextStyle(
                                        fontSize = effectiveTransSizeSp.sp,
                                        lineHeight = 1.4.em,
                                        fontWeight = FontWeight(min(700, rowWeight - 50)),
                                        letterSpacing = (letterSpacingPx * 0.5f).sp,
                                        textAlign = TextAlign.Start,
                                    ),
                                    color = Color(
                                        red = bright,
                                        green = bright,
                                        blue = bright,
                                        alpha = (0.68f + 0.20f * near).coerceIn(0f, 1f),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. 竖直左对齐虚线参考线（开启「对齐参考线」时显示，直观验证所有行起点严格落在同一竖线）
        if (showGuide) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .offset { IntOffset(padPx.roundToInt(), 0) }
            ) {
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(0f, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                        phase = 0f,
                    ),
                )
            }
        }

        // 4. 手动滚动时浮现的「回到当前歌词」胶囊按钮
        AnimatedVisibility(
            visible = isManualBrowsing,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xAA090E16))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        jumpToDisplayIndex(targetDisplayIndex, seekAudio = false)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "回到当前歌词",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 5. 可折叠实时 3D 参数调节面板（对应参考 HTML 右下角 .panel）
        AnimatedVisibility(
            visible = showQuickPanel && (controlsVisible || panelExpanded),
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(220)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 14.dp,
                    bottom = maxOf(16.dp, bottomFadeHeight * 0.25f),
                ),
        ) {
            Salt3DParameterPanel(
                expanded = panelExpanded,
                onExpandedChange = { panelExpanded = it },
                angleDeg = angleDeg,
                onAngleChange = setAngleDeg,
                depthVal = depthVal,
                onDepthChange = setDepthVal,
                perspectiveP = perspectiveP,
                onPerspectiveChange = setPerspectiveP,
                durationMs = durationMs,
                onDurationChange = setDurationMs,
                blurMaxDp = blurMaxDp,
                onBlurMaxChange = setBlurMaxDp,
                baseWeight = baseWeight,
                onBaseWeightChange = setBaseWeight,
                gapDp = gapDp,
                onGapChange = setGapDp,
                letterSpacingPx = letterSpacingPx,
                onLetterSpacingChange = setLetterSpacingPx,
                effectivePadDp = effectivePadDp,
                onPadChange = setCustomPadDp,
                traditional = traditional,
                onTraditionalChange = setTraditional,
                useComp = useComp,
                onUseCompChange = setUseComp,
                showGuide = showGuide,
                onShowGuideChange = setShowGuide,
                onPrevLine = {
                    val prev = (posAnim.value.roundToInt() - 1).coerceIn(0, lineCount - 1)
                    jumpToDisplayIndex(prev, seekAudio = true)
                },
                onNextLine = {
                    val next = (posAnim.value.roundToInt() + 1).coerceIn(0, lineCount - 1)
                    jumpToDisplayIndex(next, seekAudio = true)
                },
                onResetDefaults = {
                    setAngleDeg(40f)
                    setDepthVal(60f)
                    setPerspectiveP(900f)
                    setDurationMs(440)
                    setBlurMaxDp(5.5f)
                    setBaseWeight(600)
                    setGapDp(24f)
                    setLetterSpacingPx(0f)
                    setCustomPadDp(-1f)
                    setTraditional(false)
                    setUseComp(true)
                    setShowGuide(false)
                },
            )
        }
    }
}

/**
 * 根据屏幕触点 Y 坐标反查命中的 3D 歌词行索引
 */
private fun findHitRowIndex(
    touchY: Float,
    currentPos: Float,
    focusY: Float,
    base: Float,
    pPx: Float,
    zNearPx: Float,
    zStepPx: Float,
    shrink: Float,
    geometry: SaltMeasuredGeometry,
): Int {
    val lineCount = geometry.tops.size
    val startIdx = (floor(currentPos) - 7).toInt().coerceAtLeast(0)
    val endIdx = (floor(currentPos) + 8).toInt().coerceAtMost(lineCount - 1)
    var bestIdx = -1
    var bestDist = Float.MAX_VALUE
    for (i in startIdx..endIdx) {
        val d = i.toFloat() - currentPos
        val ad = abs(d)
        if (ad > 7.2f) continue
        val near = (1f - ad).coerceAtLeast(0f)
        val z = zNearPx * near - zStepPx * ad
        val rowTopY = base + geometry.tops[i]
        val rowH = geometry.heights[i]
        val yCenter = rowTopY + rowH * 0.5f
        val scale = (1f - shrink * ad).coerceAtLeast(0.55f)
        val perspZ = (pPx / (pPx - z).coerceAtLeast(120f)).coerceIn(0.35f, 1.35f)
        val effectiveScale = scale * perspZ
        val screenCenterY = focusY + (yCenter - focusY) * perspZ
        val halfScaledH = (rowH * effectiveScale * 0.6f).coerceAtLeast(22f)
        val dist = abs(touchY - screenCenterY)
        if (dist <= halfScaledH && dist < bestDist) {
            bestDist = dist
            bestIdx = i
        }
    }
    return bestIdx
}

private fun depthLabel(v: Float): String = when {
    v <= 0f -> "关闭"
    v < 35f -> "弱"
    v <= 75f -> "标准"
    else -> "强"
}

/**
 * 复刻参考 HTML 的可折叠毛玻璃实时参数调节面板
 */
@Composable
private fun Salt3DParameterPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    angleDeg: Float,
    onAngleChange: (Float) -> Unit,
    depthVal: Float,
    onDepthChange: (Float) -> Unit,
    perspectiveP: Float,
    onPerspectiveChange: (Float) -> Unit,
    durationMs: Int,
    onDurationChange: (Int) -> Unit,
    blurMaxDp: Float,
    onBlurMaxChange: (Float) -> Unit,
    baseWeight: Int,
    onBaseWeightChange: (Int) -> Unit,
    gapDp: Float,
    onGapChange: (Float) -> Unit,
    letterSpacingPx: Float,
    onLetterSpacingChange: (Float) -> Unit,
    effectivePadDp: Float,
    onPadChange: (Float) -> Unit,
    traditional: Boolean,
    onTraditionalChange: (Boolean) -> Unit,
    useComp: Boolean,
    onUseCompChange: (Boolean) -> Unit,
    showGuide: Boolean,
    onShowGuideChange: (Boolean) -> Unit,
    onPrevLine: () -> Unit,
    onNextLine: () -> Unit,
    onResetDefaults: () -> Unit,
) {
    val bgColor = if (expanded) Color(0xCC090E16) else Color(0x7A090E16)
    val borderColor = if (expanded) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.10f)

    Column(
        modifier = Modifier
            .widthIn(max = if (expanded) 244.dp else 156.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                // 拦截面板内部的指针事件，避免穿透到底层歌词舞台
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "参数 / 方案（点击收起）" else "参数 / 方案",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                letterSpacing = 0.06.em,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SaltPanelButton("上一句", Modifier.weight(1f), onPrevLine)
                    SaltPanelButton("下一句", Modifier.weight(1f), onNextLine)
                    SaltPanelButton("默认", Modifier.weight(1f), onResetDefaults)
                }

                Spacer(Modifier.height(8.dp))

                SaltPanelSlider(
                    label = "侧向角度",
                    valueText = "${angleDeg.roundToInt()}°",
                    value = angleDeg,
                    valueRange = 0f..55f,
                    onValueChange = { onAngleChange(it.roundToInt().toFloat()) },
                )
                SaltPanelSlider(
                    label = "空间景深强度",
                    valueText = "${depthVal.roundToInt()}% · ${depthLabel(depthVal)}",
                    value = depthVal,
                    valueRange = 0f..100f,
                    onValueChange = { onDepthChange((it / 5f).roundToInt() * 5f) },
                )
                SaltPanelSlider(
                    label = "透视距离 P",
                    valueText = "${perspectiveP.roundToInt()}px",
                    value = perspectiveP,
                    valueRange = 500f..1600f,
                    onValueChange = { onPerspectiveChange((it / 20f).roundToInt() * 20f) },
                )
                SaltPanelSlider(
                    label = "换行时长",
                    valueText = "${durationMs}ms",
                    value = durationMs.toFloat(),
                    valueRange = 200f..900f,
                    onValueChange = { onDurationChange((it / 20f).roundToInt() * 20) },
                )
                SaltPanelSlider(
                    label = "最远模糊",
                    valueText = "${"%.1f".format(blurMaxDp)}px",
                    value = blurMaxDp,
                    valueRange = 0f..9f,
                    onValueChange = { onBlurMaxChange((it * 2f).roundToInt() / 2f) },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.White.copy(alpha = 0.10f),
                )
                Text(
                    text = "文字 / 排版",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                    letterSpacing = 0.12.em,
                )

                SaltPanelSlider(
                    label = "字体粗细",
                    valueText = "$baseWeight",
                    value = baseWeight.toFloat(),
                    valueRange = 400f..900f,
                    onValueChange = { onBaseWeightChange(((it / 50f).roundToInt() * 50).coerceIn(400, 900)) },
                )
                SaltPanelSlider(
                    label = "行间距",
                    valueText = "${gapDp.roundToInt()}px",
                    value = gapDp,
                    valueRange = 0f..64f,
                    onValueChange = { onGapChange((it / 2f).roundToInt() * 2f) },
                )
                SaltPanelSlider(
                    label = "字间距",
                    valueText = "${"%.1f".format(letterSpacingPx)}px",
                    value = letterSpacingPx,
                    valueRange = 0f..4f,
                    onValueChange = { onLetterSpacingChange((it * 5f).roundToInt() / 5f) },
                )
                SaltPanelSlider(
                    label = "左缩进",
                    valueText = "${effectivePadDp.roundToInt()}px",
                    value = effectivePadDp.coerceIn(0f, 160f),
                    valueRange = 0f..160f,
                    onValueChange = { onPadChange((it / 2f).roundToInt() * 2f) },
                )

                Spacer(Modifier.height(6.dp))

                SaltPanelCheckRow("繁体歌词", traditional, onTraditionalChange)
                SaltPanelCheckRow("起始补偿 (解析左对齐)", useComp, onUseCompChange)
                SaltPanelCheckRow("对齐参考线", showGuide, onShowGuideChange)
            }
        }
    }
}

@Composable
private fun SaltPanelButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFFEAF2F6),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SaltPanelSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
            )
            Text(
                text = valueText,
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF8FB8C9),
                activeTrackColor = Color(0xFF8FB8C9),
                inactiveTrackColor = Color.White.copy(alpha = 0.20f),
            ),
        )
    }
}

@Composable
private fun SaltPanelCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF8FB8C9),
                uncheckedColor = Color.White.copy(alpha = 0.45f),
                checkmarkColor = Color(0xFF090E16),
            ),
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
        )
    }
}
