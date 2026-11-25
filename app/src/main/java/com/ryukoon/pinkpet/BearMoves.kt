package com.ryukoon.pinkpet

/**
 * 여유곰 일반 모션.
 * events: (시간초, 명령) — "say:글", "boing:글", "fl:글", "shake", "flash", "pop"
 */
private fun P(
    hx: Int = 0, hy: Int = 0, by: Int = 12, ly: Int = 18, legs: String = "stand", aL: String = "down", aR: String = "down",
    eyes: String = "open", look: Int = 1, mouth: String = "flat", blush: Boolean = false, lying: Boolean = false,
    prop: String? = null, sx: Float = 1f, sy: Float = 1f, rot: Float = 0f
) = Pose(hx = hx, hy = hy, by = by, ly = ly, legs = legs, aL = aL, aR = aR, eyes = eyes, look = look, mouth = mouth,
    blush = blush, lying = lying, prop = prop, sx = sx, sy = sy, rot = rot)

private fun S(eyes: String = "open", mouth: String = "flat", hy: Int = 2, aL: String = "down", aR: String = "down", prop: String? = null, blush: Boolean = false, look: Int = 1) =
    Pose(by = 14, hy = hy, ly = 20, legs = "sit", eyes = eyes, mouth = mouth, aL = aL, aR = aR, prop = prop, blush = blush, look = look)

private fun L(eyes: String = "closed", mouth: String = "flat", aL: String = "down", aR: String = "down", prop: String? = null, blush: Boolean = false) =
    Pose(lying = true, eyes = eyes, mouth = mouth, aL = aL, aR = aR, prop = prop, blush = blush)

object BearMoves {
    val all: LinkedHashMap<String, Move> = linkedMapOf(
        // ---- 곰다운 모션 ----
        "pawlick" to Move("🐾 곰발바닥 핥기", 5f, 2.5f, listOf(S(aR = "paw", mouth = "tongue", eyes = "closed", blush = true), S(aR = "paw", mouth = "tongue", eyes = "closed", hy = 3)),
            w = 1f, says = listOf("할짝할짝… 꿀맛", "곰발바닥은 달콤해"), fx = "💕", fxEvery = .9f),
        "pawlickStand" to Move("🐾 서서 발바닥 냠냠", 3.5f, 3f, listOf(P(aR = "paw", mouth = "tongue", eyes = "closed"), P(aR = "paw", mouth = "smile", eyes = "closed", hy = 1, by = 13)), w = .7f, fx = "♡", fxEvery = .6f),
        "hibernate" to Move("겨울잠 (푹 자기)", 14f, .6f, listOf(L(mouth = "o", prop = "pillow"), L(mouth = "flat", prop = "pillow")), w = .9f, fx = "z", fxEvery = .7f, fxKind = "z",
            events = listOf(.2f to "say:겨울잠 잘 시간…", 4f to "say:드르렁~ 쿨…", 8f to "say:음냐음냐… 꿀…", 12f to "say:드르렁~~")),
        "nap" to Move("꾸벅 낮잠", 6f, 1f, listOf(S(eyes = "closed", hy = 2), S(eyes = "closed", hy = 4)), w = .6f, fx = "z", fxEvery = .8f, fxKind = "z"),
        "sunbath" to Move("햇볕 쬐기", 6f, .5f, listOf(L(mouth = "smile", aL = "up", aR = "up", blush = true)), fx = "☀", fxEvery = 1f, says = listOf("아~ 따뜻하다")),
        "honey" to Move("🍯 꿀 먹기", 5f, 2f, listOf(P(aR = "hold", prop = "honey", mouth = "open"), P(aR = "hold", prop = "honey", eyes = "closed", mouth = "tongue", blush = true)),
            w = .8f, says = listOf("꿀은 못 참지", "달달하다~"), fx = "🍯", fxEvery = 1f),
        "honeyDip" to Move("꿀단지에 발 넣기", 4f, 2f, listOf(P(aR = "hip", aL = "hold", prop = "honey"), P(aR = "paw", aL = "hold", mouth = "tongue", eyes = "closed")), says = listOf("한 번만 찍어 먹자…")),
        "salmon" to Move("🐟 연어 낚기", 4f, 3f, listOf(P(aR = "up", eyes = "angry", mouth = "flat", look = 1), P(aR = "out", eyes = "wide", mouth = "o", by = 13, hy = 2)),
            w = .8f, switchAt = 2.4f, frames2 = listOf(P(aR = "up", prop = "salmon", eyes = "sparkle", mouth = "grin")), events = listOf(.2f to "say:연어 어딨지…", 2.4f to "boing:잡았다! 🐟", 2.4f to "pop")),
        "salmonEat" to Move("연어 냠냠", 4f, 2.5f, listOf(P(aR = "hold", prop = "salmon", mouth = "open"), P(aR = "hold", prop = "salmon", mouth = "flat", eyes = "closed", blush = true)), fx = "🐟", fxEvery = 1f),
        "berry" to Move("산딸기 따기", 4f, 2f, listOf(P(aR = "up", look = -1), P(aR = "hold", prop = "berry", mouth = "smile")), says = listOf("산딸기 발견!"), fx = "🍓", fxEvery = .9f),
        "backscratch" to Move("등 긁기", 4f, 4f, listOf(P(eyes = "closed", mouth = "smile", rot = -6f, aL = "up"), P(eyes = "closed", mouth = "o", rot = 6f, aL = "up")), w = .7f, says = listOf("아 거기 거기… 시원하다~")),
        "roar" to Move("어흥!", 2.5f, 1f, listOf(P(aL = "up", aR = "up", mouth = "open", eyes = "angry", sy = 1.1f)), jitter = true, events = listOf(.2f to "boing:어흥~!!", .2f to "shake")),
        "standTall" to Move("두 발로 우뚝", 3.5f, .8f, listOf(P(aL = "paws", aR = "paws", sy = 1.1f, look = 1), P(aL = "paws", aR = "paws", sy = 1.1f, look = -1)), says = listOf("킁… 뭐가 있나?")),
        "bellydrum" to Move("배 두드리기", 3.5f, 6f, listOf(P(aL = "hold", aR = "hip", mouth = "grin", eyes = "closed"), P(aL = "hip", aR = "hold", mouth = "smile")), fx = "둥", fxEvery = .3f, says = listOf("배부르다~ 둥둥")),
        "sniff" to Move("킁킁 냄새", 3.5f, 2f, listOf(P(look = 1, hx = 1, mouth = "o"), P(look = -1, hx = -1, mouth = "o")), fx = "킁", fxEvery = .5f, says = listOf("어디서 치킨 냄새가…")),
        "dig" to Move("땅 파기", 4f, 6f, listOf(P(by = 13, hy = 2, aL = "paws", aR = "out", sy = .92f), P(by = 13, hy = 2, aL = "out", aR = "paws", sy = .92f)), fx = "·", fxEvery = .2f, says = listOf("뭔가 묻혀 있다!")),
        "climb" to Move("나무 타기", 4f, 4f, listOf(P(aL = "up", aR = "paws", legs = "tuck"), P(aL = "paws", aR = "up", legs = "dangle")), float = 10f, says = listOf("영차영차")),
        "pawPrint" to Move("🐾 발도장 쾅쾅", 3.5f, 3f, listOf(P(legs = "kick", mouth = "smile"), P(legs = "stand", mouth = "grin", sy = .94f)), hop = 3f, hopRate = 1.5f, fx = "🐾", fxEvery = .35f),
        "pawWave" to Move("🐾 곰발 흔들기", 3f, 4f, listOf(P(aR = "wave", mouth = "smile", blush = true), P(aR = "up", mouth = "smile", blush = true)), says = listOf("곰발바닥 보여줄게~")),
        "hug" to Move("꼬옥 포옹", 3.5f, 1.2f, listOf(P(aL = "out", aR = "out", mouth = "smile"), P(aL = "hold", aR = "hold", eyes = "closed", mouth = "smile", blush = true)), fx = "💕", fxEvery = .4f, says = listOf("곰 포옹~ 꼬옥")),
        "bootyshake" to Move("엉덩이 씰룩", 3f, 6f, listOf(P(rot = -8f, mouth = "grin", aL = "hip", aR = "hip"), P(rot = 8f, mouth = "grin", aL = "hip", aR = "hip")), says = listOf("씰룩씰룩~")),
        "bees" to Move("벌 떼 도망", 3.5f, 10f, listOf(P(legs = "runA", aL = "up", aR = "up", mouth = "open", eyes = "wide"), P(legs = "walkB", aL = "up", aR = "up", mouth = "open", eyes = "wide")),
            move = 220f, fx = "🐝", fxEvery = .25f, says = listOf("으악 벌이다!! 꿀 안 훔쳤어!!")),
        // ---- 추위 안 타요 ----
        "snowball" to Move("❄ 눈싸움", 4f, 2f, listOf(P(aR = "up", prop = "snowball", mouth = "grin"), P(aR = "out", mouth = "open")), fx = "❄", fxEvery = .5f, says = listOf("받아라~ 눈뭉치!")),
        "snowangel" to Move("❄ 눈천사", 5f, 2f, listOf(L(aL = "up", aR = "up", mouth = "smile", eyes = "open"), L(aL = "out", aR = "out", mouth = "smile", eyes = "open")), fx = "❄", fxEvery = .5f, says = listOf("눈밭 최고~")),
        "snowman" to Move("⛄ 눈사람 만들기", 5f, 2f, listOf(S(aL = "out", aR = "hold", prop = "snowball"), S(aL = "hold", aR = "out", mouth = "smile")), events = listOf(3.5f to "boing:⛄ 완성!")),
        "notCold" to Move("하나도 안 추움", 4f, 1f, listOf(P(aL = "hip", aR = "hip", mouth = "smile", eyes = "closed")), fx = "❄", fxEvery = .35f, says = listOf("이 정도는 따뜻하지~", "곰은 추위 안 타요")),
        "iceSlide" to Move("얼음 미끄럼", 2.5f, 1f, listOf(P(aL = "out", aR = "out", legs = "walkA", mouth = "grin", rot = 6f)), move = 260f, fx = "✨", fxEvery = .2f, says = listOf("슈우웅~")),
        "snowShake" to Move("부르르 눈 털기", 2.5f, 1f, listOf(P(eyes = "closed", mouth = "grin")), jitter = true, fx = "❄", fxEvery = .15f, events = listOf(.1f to "say:부르르르~")),
        "icecream" to Move("겨울 아이스크림", 4f, 2f, listOf(P(aR = "hold", prop = "icecream", mouth = "tongue"), P(aR = "hold", prop = "icecream", mouth = "smile", eyes = "closed", blush = true)), says = listOf("겨울엔 아이스크림이지")),
        "santa" to Move("🎅 산타곰", 4f, 2f, listOf(P(prop = "santa", aR = "wave", mouth = "grin"), P(prop = "santa", aR = "up", mouth = "smile")), says = listOf("메리 크리스마스~!"), fx = "🎁", fxEvery = .8f),
        // ---- 여유로운 일상 ----
        "chill" to Move("주머니에 손 여유", 5f, .5f, listOf(P(eyes = "open", mouth = "smile"), P(eyes = "closed", mouth = "smile")), w = .8f, says = listOf("여유롭다~", "서두를 거 없지")),
        "stroll" to Move("느긋한 산책", 6f, 3f, listOf(P(legs = "walkA", mouth = "smile"), P(legs = "stand"), P(legs = "walkB", mouth = "smile"), P(legs = "stand")), move = 35f, w = .8f, fx = "♪", fxEvery = 1f),
        "spaceout" to Move("멍때리기", 5f, .3f, listOf(P(look = -1, mouth = "o"), P(look = 1, mouth = "o")), fx = "…", fxEvery = 1.4f),
        "sky" to Move("하늘 구경", 4f, .5f, listOf(P(hy = -1, eyes = "sparkle", mouth = "smile")), fx = "☁", fxEvery = 1f, says = listOf("하늘 예쁘다~")),
        "hum" to Move("콧노래", 4f, 2f, listOf(P(eyes = "closed", mouth = "smile", rot = -3f), P(eyes = "closed", mouth = "o", rot = 3f)), fx = "♪", fxEvery = .45f),
        "shrug" to Move("어깨 으쓱", 2.5f, 2f, listOf(P(aL = "out", aR = "out", mouth = "smile", sy = 1.04f), P(mouth = "smile")), says = listOf("뭐 어때~", "될 대로 되겠지")),
        "thumbsup" to Move("엄지척", 2.5f, 1f, listOf(P(aR = "up", eyes = "wink", mouth = "grin")), says = listOf("👍 최고!")),
        "coffee" to Move("아아 한잔", 5f, 1.2f, listOf(P(aR = "hold", prop = "coffee"), P(aR = "hold", prop = "coffee", eyes = "closed", mouth = "o", hy = -1)), fx = "~", fxEvery = .7f, fxKind = "z", says = listOf("아아는 사계절 내내")),
        "phone" to Move("폰 보기", 5f, 1.5f, listOf(P(aR = "hold", prop = "phone", hy = 1, look = -1), P(aR = "hold", prop = "phone", hy = 1, mouth = "grin", eyes = "closed")), fx = "ㅋ", fxEvery = .8f),
        "lunch" to Move("도시락", 5f, 2f, listOf(S(aR = "hold", prop = "lunchbox", mouth = "open"), S(aR = "hold", prop = "lunchbox", eyes = "closed", blush = true)), says = listOf("점심시간이 제일 좋아")),
        "cupramen" to Move("컵라면 야식", 5f, 2.5f, listOf(S(aR = "hold", prop = "cupramen", mouth = "open"), S(aR = "hold", prop = "cupramen", eyes = "closed")), fx = "~", fxEvery = .6f, fxKind = "z", says = listOf("후루룩~ 야식은 못 참지")),
        "chickenEat" to Move("치킨 먹방", 5f, 2.5f, listOf(P(aR = "hold", prop = "drumstick", mouth = "open"), P(aR = "hold", prop = "drumstick", eyes = "closed", blush = true)), fx = "🍗", fxEvery = .9f, says = listOf("치킨은 진리…")),
        "firewall" to Move("방화벽 정책 조회", 6f, 4f, listOf(S(prop = "laptop"), S(prop = "laptop", look = -1)), fx = "타닥", fxEvery = .5f,
            says = listOf("정책 조회 중…", "이 포트 누가 열었어…")),
        "study" to Move("자격증 공부", 6f, .5f, listOf(S(aR = "hold", prop = "book", hy = 3), S(aR = "hold", prop = "book", hy = 3, look = -1)), says = listOf("보안기사 합격하자!", "외울 게 너무 많아…"), fx = "✏", fxEvery = 1.2f),
        "bike" to Move("🚲 자전거 타기", 5f, 6f, listOf(P(prop = "bike", by = 10, hy = -2, ly = 15, legs = "walkA", aL = "out", aR = "out", mouth = "grin"), P(prop = "bike", by = 10, hy = -2, ly = 15, legs = "walkB", aL = "out", aR = "out", mouth = "smile")),
            move = 160f, w = .6f, says = listOf("장거리 라이딩 간다~"), fx = "💨", fxEvery = .5f),
        "offwork" to Move("칼퇴근", 4f, 8f, listOf(P(legs = "walkA", aR = "wave", mouth = "grin"), P(legs = "walkB", aR = "up", mouth = "grin")), move = 110f, says = listOf("칼퇴!! 내일 봐요 👋")),
        "doze" to Move("업무 중 꾸벅", 5f, 1.2f, listOf(S(eyes = "closed", hy = 2), S(eyes = "closed", hy = 4)), fx = "z", fxEvery = .9f, fxKind = "z",
            switchAt = 4.2f, frames2 = listOf(S(eyes = "wide", mouth = "o", hy = 1)), events = listOf(4.2f to "boing:헉! 안 잤어요!")),
        "selfie" to Move("셀카", 3f, 2f, listOf(P(aR = "up", prop = "phone", mouth = "smile", eyes = "wink"), P(aR = "up", prop = "phone", mouth = "grin", blush = true)),
            events = listOf(1.2f to "flash", 1.2f to "fl:찰칵!", 2.2f to "flash", 2.2f to "fl:찰칵!")),
        "camera" to Move("사진 찍기", 3f, 1f, listOf(P(aR = "hold", aL = "hold", prop = "camera", eyes = "wink")), events = listOf(1.3f to "flash", 1.3f to "fl:찰칵!", 2.2f to "say:잘 나왔다!")),
        "music" to Move("음악 감상", 5f, 2.5f, listOf(P(eyes = "closed", mouth = "smile", prop = "headphones"), P(eyes = "closed", mouth = "smile", by = 13, hy = 1, prop = "headphones")), fx = "♪", fxEvery = .45f),
        "sing" to Move("노래방", 5f, 3f, listOf(P(aR = "hold", prop = "mic", mouth = "open", aL = "out"), P(aR = "hold", prop = "mic", mouth = "o", eyes = "closed", aL = "up")), fx = "♪", fxEvery = .4f, says = listOf("♬ 곰 세 마리가~")),
        "guitar" to Move("기타 연주", 5f, 4f, listOf(P(aR = "hold", aL = "out", prop = "guitar", eyes = "closed", mouth = "smile"), P(aR = "hip", aL = "out", prop = "guitar", eyes = "closed", mouth = "o")), fx = "♪", fxEvery = .35f),
        "flower" to Move("꽃 향기", 4f, .8f, listOf(P(aR = "hold", prop = "flower", eyes = "closed", mouth = "smile", blush = true)), fx = "🌸", fxEvery = .8f, says = listOf("음~ 좋은 향기")),
        "balloon" to Move("풍선 둥실", 5f, 1f, listOf(P(aR = "out", prop = "balloon", mouth = "smile")), float = 8f, says = listOf("둥실둥실~")),
        // ---- 운동 ----
        "workout" to Move("아령 운동", 4f, 2f, listOf(P(aL = "up", aR = "up", prop = "dumbbell", mouth = "grin", eyes = "angry"), P(aL = "paws", aR = "paws", prop = "dumbbellDown", mouth = "o", by = 13, hy = 1)), fx = "💦", fxEvery = .8f, says = listOf("곰은 힘이 세지!")),
        "squat" to Move("스쿼트", 4f, 2f, listOf(P(aL = "out", aR = "out"), P(by = 14, hy = 3, legs = "tuck", ly = 19, aL = "out", aR = "out", mouth = "grin", eyes = "angry")), fx = "💦", fxEvery = 1f),
        "yoga" to Move("곰 요가", 5f, .6f, listOf(P(aL = "up", aR = "up", legs = "kick", eyes = "closed"), P(aL = "out", aR = "out", legs = "kick", eyes = "closed", rot = 4f)), says = listOf("나마스떼…")),
        "meditate" to Move("명상(공중부양)", 6f, .5f, listOf(S(eyes = "closed", aL = "out", aR = "out")), float = 6f, fx = "✨", fxEvery = .7f, says = listOf("옴~ 여유~")),
        "kungfu" to Move("곰발 쿵푸", 3.5f, 3f, listOf(P(legs = "kick", aR = "out", aL = "hip", eyes = "angry", mouth = "open"), P(legs = "walkA", aL = "out", aR = "hip", eyes = "angry")), fx = "곰발 펀치!", fxEvery = .9f),
        "boxing" to Move("곰 펀치", 3.5f, 6f, listOf(P(aR = "out", aL = "hold", legs = "walkA", eyes = "angry", hx = 1), P(aR = "hold", aL = "out", legs = "walkA", eyes = "angry")), fx = "퍽!", fxEvery = .45f),
        "jumprope" to Move("줄넘기", 4f, 4f, listOf(P(aL = "out", aR = "out", legs = "tuck"), P(aL = "hip", aR = "hip")), hop = 4f, hopRate = 2f,
            events = listOf(.5f to "fl:1", 1f to "fl:2", 1.5f to "fl:3", 2f to "fl:4", 2.5f to "fl:5")),
        "jumpingjack" to Move("팔벌려뛰기", 4f, 4f, listOf(P(aL = "up", aR = "up", legs = "walkA", mouth = "open"), P(legs = "stand")), hop = 3f, hopRate = 4f, fx = "💦", fxEvery = .9f),
        // ---- 감정 ----
        "laugh" to Move("껄껄 웃기", 3.5f, 6f, listOf(P(eyes = "closed", mouth = "grin", aL = "hip", aR = "hip"), P(eyes = "closed", mouth = "open", aL = "hip", aR = "hip", hy = 1, by = 13)), fx = "ㅋ", fxEvery = .3f, says = listOf("껄껄껄!")),
        "moved" to Move("감동 눈물", 4f, 2f, listOf(P(eyes = "tear", mouth = "smile", aL = "hold", aR = "hold")), fx = "💧", fxEvery = .6f, says = listOf("흑… 너무 감동이야")),
        "pout" to Move("살짝 삐짐", 3.5f, 1f, listOf(P(eyes = "closed", mouth = "o", look = -1, hx = -1, rot = -4f, aL = "hold", aR = "hold")), says = listOf("흥… 삐졌어", "…안 삐졌는데?")),
        "shy" to Move("부끄부끄", 3.5f, 2f, listOf(P(aL = "hold", aR = "hold", eyes = "closed", blush = true, mouth = "smile", hx = -1, rot = -4f), P(aL = "hold", aR = "hold", eyes = "closed", blush = true, mouth = "smile", hx = 1, rot = 4f)), says = listOf("아잉…")),
        "love" to Move("하트 뿅뿅", 3.5f, 1.5f, listOf(P(aL = "up", aR = "up", eyes = "closed", mouth = "smile", blush = true), P(aL = "out", aR = "out", eyes = "wink", mouth = "smile", prop = "heart")), fx = "💕", fxEvery = .35f, says = listOf("사랑해요~")),
        "wink" to Move("윙크", 2.5f, 1f, listOf(P(eyes = "wink", mouth = "smile", aR = "wave", blush = true)), says = listOf("😉 찡긋")),
        "think" to Move("생각 중", 4f, .7f, listOf(P(aR = "hold", look = -1, hy = -1), P(aR = "hold", look = 1, eyes = "closed")), fx = "💭", fxEvery = 1.2f, says = listOf("흠…")),
        "idea" to Move("번뜩 아이디어", 3f, .7f, listOf(P(aR = "hold", look = -1), P(aR = "hold", eyes = "closed")),
            switchAt = 1.6f, frames2 = listOf(P(aR = "up", eyes = "sparkle", mouth = "grin", sy = 1.06f)), events = listOf(1.6f to "boing:💡 아하!", 1.6f to "pop")),
        "cheer" to Move("화이팅", 3.5f, 4f, listOf(P(aL = "up", aR = "out", mouth = "open"), P(aL = "out", aR = "up", mouth = "grin")), hop = 3f, hopRate = 2f, says = listOf("화이팅!!", "오늘도 잘 될 거야!")),
        "clap" to Move("박수", 3f, 6f, listOf(P(aL = "hold", aR = "hold", mouth = "grin", eyes = "closed"), P(aL = "out", aR = "out", mouth = "grin")), fx = "👏", fxEvery = .35f),
        "bow" to Move("꾸벅 인사", 2.5f, 1.2f, listOf(P(mouth = "smile"), P(hy = 3, by = 13, eyes = "closed", mouth = "smile", sy = .92f)), says = listOf("안녕하세요~", "수고하셨습니다!")),
        // ---- 웃긴 것 ----
        "sneeze" to Move("에취", 2.6f, 2f, listOf(P(eyes = "closed", mouth = "o", hy = -1, by = 11), P(eyes = "closed", mouth = "open", hy = -1, by = 11)),
            switchAt = 1.5f, frames2 = listOf(P(eyes = "closed", mouth = "open", hy = 2, by = 13, aL = "out", aR = "out", sy = .85f, sx = 1.15f)),
            events = listOf(.1f to "say:아… 아…", 1.5f to "boing:에취!!", 1.5f to "shake")),
        "hiccup" to Move("딸꾹질", 4f, 1f, listOf(P(eyes = "wide", mouth = "o")), hop = 4f, hopRate = .9f,
            events = listOf(.5f to "fl:딸꾹!", 1.6f to "fl:딸꾹!", 2.8f to "fl:딸꾹!")),
        "yawn" to Move("하품 쩍", 3f, 1f, listOf(P(aR = "paw", eyes = "closed", mouth = "open", sy = 1.05f)), says = listOf("하아아암…")),
        "spin" to Move("빙글빙글", 3f, 1f, listOf(P(aL = "out", aR = "out", mouth = "open", eyes = "closed")), spin = 14f, events = listOf(2.9f to "fl:💫")),
        "roll" to Move("데굴데굴", 3f, 1f, listOf(P(legs = "tuck", aL = "up", aR = "up", eyes = "closed", mouth = "open")), roll = 540f, move = 120f, events = listOf(2.9f to "say:어지러…")),
        "moonwalk" to Move("문워크", 4f, 5f, listOf(P(legs = "walkA", aL = "hip", aR = "out", mouth = "smile"), P(legs = "walkB", aL = "hip", aR = "out", mouth = "o")), move = 70f, back = true, fx = "♪", fxEvery = .4f),
        "robot" to Move("로봇 춤", 4f, 3f, listOf(P(aL = "out", aR = "down", legs = "walkA"), P(aL = "down", aR = "out", legs = "walkB", hx = 1), P(aL = "up", aR = "up")), says = listOf("삐-빅. 곰봇 작동.")),
        "trip" to Move("꽈당", 3.5f, 6f, listOf(P(legs = "walkA", mouth = "smile"), P(legs = "walkB")), move = 90f,
            switchAt = 1f, frames2 = listOf(L(eyes = "wide", mouth = "open")), events = listOf(1f to "boing:꽈당!", 1f to "shake", 2.2f to "say:괜찮아… 곰은 튼튼해")),
        "sleepwalk" to Move("몽유병", 5f, 2f, listOf(P(aL = "out", aR = "out", eyes = "closed", legs = "walkA", mouth = "o"), P(aL = "out", aR = "out", eyes = "closed", legs = "walkB")), move = 25f, fx = "z", fxEvery = .8f, fxKind = "z"),
        "hide" to Move("숨바꼭질", 5f, 1f, listOf(P(aL = "hold", aR = "hold", eyes = "closed")),
            switchAt = 3.5f, frames2 = listOf(P(aR = "out", eyes = "wide", mouth = "grin")), events = listOf(.2f to "say:하나… 둘… 셋…", 3.5f to "boing:찾았다!")),
        "salute" to Move("충성", 2.5f, 1f, listOf(P(aR = "wave", eyes = "angry", sy = 1.04f)), says = listOf("충성!"))
    )
}
