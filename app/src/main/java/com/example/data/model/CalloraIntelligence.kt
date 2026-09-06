package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Battlecard(
    val id: String,
    val title: String,
    val triggerTag: String, // e.g. "Pricing", "Timeline", "Contract", "Scope"
    val objectionSummary: String,
    val recommendedResponse: String,
    val bulletPoints: List<String>,
    val quickAnswer: String
)

object CalloraBattlecards {
    val defaultBattlecards = listOf(
        Battlecard(
            id = "pricing_budget",
            title = "Budget & Pricing Pushback",
            triggerTag = "Pricing",
            objectionSummary = "Client states: 'Your quote is higher than we budgeted for this quarter.'",
            recommendedResponse = "Anchor on projected ROI. Frame the investment around cost of delay and offer a phased roadmap.",
            bulletPoints = listOf(
                "Validate budget constraint: 'Understood, what target number did the team have in mind?'",
                "Break total down to monthly value: e.g. 'At $4,500/mo, generating 3 qualified deals recovers the fee.'",
                "Offer a Phase 1 pilot sprint rather than discounting base agency rates."
            ),
            quickAnswer = "We can structure this as a Phase 1 MVP sprint to prove ROI before committing to the full annual retainer."
        ),
        Battlecard(
            id = "timeline_capacity",
            title = "Timeline / 'We're too busy right now'",
            triggerTag = "Timeline",
            objectionSummary = "Client says: 'We love this, but don't have bandwidth to manage another vendor right now.'",
            recommendedResponse = "Highlight white-glove agency onboarding; your team takes on 90% of operational burden.",
            bulletPoints = listOf(
                "Emphasize turnkey execution: 'You only need 45 minutes for our kick-off questionnaire.'",
                "Explain dedicated account manager handles weekly execution and reporting autonomously.",
                "Lock in schedule now: onboarding slots book 3 weeks in advance."
            ),
            quickAnswer = "Our onboarding is designed to be turnkey—you spend under 1 hour with us, and our team handles all setup."
        ),
        Battlecard(
            id = "contract_duration",
            title = "Contract Lock-in / Term Commitment",
            triggerTag = "Contract",
            objectionSummary = "Client hesitates: 'We prefer not to sign a 12-month agreement without knowing results.'",
            recommendedResponse = "Offer a 90-day checkpoint with clear KPI targets before continuing.",
            bulletPoints = listOf(
                "Propose a 90-day pilot term with a 30-day notice clause.",
                "Agree upon specific 60-day milestone deliverables in writing.",
                "Align incentives: our retention is 92% because we hit milestones."
            ),
            quickAnswer = "Let's begin with an initial 90-day pilot. If we don't hit the agreed milestones, you have no further obligation."
        ),
        Battlecard(
            id = "competitor_compare",
            title = "Competitor Comparison",
            triggerTag = "Competitor",
            objectionSummary = "Client asks: 'How are you different from Agency X who quoted 30% less?'",
            recommendedResponse = "Highlight senior in-house talent, custom reporting, and direct Slack/WhatsApp access.",
            bulletPoints = listOf(
                "Differentiate on senior execution (no junior hand-offs).",
                "Transparent weekly KPI dashboard with recorded call audits.",
                "Direct team Slack channel & priority WhatsApp hotline."
            ),
            quickAnswer = "Unlike volume agencies that delegate to junior staff, your account is run directly by senior specialists with transparent weekly audits."
        ),
        Battlecard(
            id = "scope_protection",
            title = "Out-of-Scope Requests",
            triggerTag = "Scope",
            objectionSummary = "Client requests additional campaigns or features during kickoff.",
            recommendedResponse = "Enthusiastically accept into the roadmap as an add-on or Phase 2 milestone.",
            bulletPoints = listOf(
                "Never say a flat 'no'; frame as a roadmap priority.",
                "Log it as an agreed action item for Phase 2 sprint estimate.",
                "Keep the current launch deadline protected."
            ),
            quickAnswer = "That's a fantastic idea. Let's document it for our Phase 2 release so we don't delay our scheduled Phase 1 launch date."
        )
    )
}

data class LiveCoachingMetrics(
    val talkPercentage: Int = 38,
    val listenPercentage: Int = 62,
    val wordsPerMinute: Int = 135,
    val paceLabel: String = "Optimal Pacing",
    val monologueDurationSec: Int = 18,
    val questionsAskedByAgent: Int = 4,
    val questionsAskedByClient: Int = 3
)

data class LiveTranscriptSnippet(
    val id: String = java.util.UUID.randomUUID().toString(),
    val speaker: String, // "Agency" or "Client"
    val text: String,
    val timestampMs: Long,
    val isQuestion: Boolean = false,
    val detectedTag: String? = null
)
