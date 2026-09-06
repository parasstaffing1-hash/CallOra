package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AgencyClient
import com.example.data.model.CallRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CallRecording::class, AgencyClient::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun clientDao(): ClientDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agency_call_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database.clientDao(), database.callDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(clientDao: ClientDao, callDao: CallDao) {
            val initialClients = listOf(
                AgencyClient(
                    name = "Sarah Jenkins",
                    company = "Apex Growth Partners",
                    phone = "+1 (555) 234-5678",
                    email = "sarah@apexgrowth.io",
                    stage = "Active Retainer",
                    monthlyValue = "$8,500/mo",
                    avatarColorHex = "#6366F1",
                    totalCallsCount = 3,
                    lastCallTimestamp = System.currentTimeMillis() - 3600000 * 2
                ),
                AgencyClient(
                    name = "David Sterling",
                    company = "Nimbus Cloud Systems",
                    phone = "+1 (555) 876-5432",
                    email = "d.sterling@nimbuscloud.tech",
                    stage = "Contract & Negotiation",
                    monthlyValue = "$14,000/mo",
                    avatarColorHex = "#06B6D4",
                    totalCallsCount = 2,
                    lastCallTimestamp = System.currentTimeMillis() - 3600000 * 24
                ),
                AgencyClient(
                    name = "Elena Rostova",
                    company = "Vanguard Digital Media",
                    phone = "+1 (555) 432-1098",
                    email = "elena@vanguarddigital.co",
                    stage = "Discovery & Pitch",
                    monthlyValue = "$6,200/mo",
                    avatarColorHex = "#10B981",
                    totalCallsCount = 1,
                    lastCallTimestamp = System.currentTimeMillis() - 3600000 * 48
                ),
                AgencyClient(
                    name = "Marcus Vance",
                    company = "Horizon Capital",
                    phone = "+1 (555) 901-2345",
                    email = "marcus@horizoncap.fund",
                    stage = "Proposal Sent",
                    monthlyValue = "$18,500/mo",
                    avatarColorHex = "#F59E0B",
                    totalCallsCount = 1,
                    lastCallTimestamp = System.currentTimeMillis() - 3600000 * 72
                )
            )
            clientDao.insertClients(initialClients)

            // Seed 2 realistic agency calls
            val sampleCall1 = CallRecording(
                title = "Apex Growth - Q4 Campaign Strategy & Budget Review",
                clientName = "Sarah Jenkins",
                clientCompany = "Apex Growth Partners",
                clientPhone = "+1 (555) 234-5678",
                clientEmail = "sarah@apexgrowth.io",
                callType = com.example.data.model.CallType.STATUS_UPDATE,
                direction = com.example.data.model.CallDirection.OUTBOUND,
                audioFilePath = "",
                durationMs = 432000L, // 7m 12s
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                fileSize = 4850000L,
                isStarred = true,
                notes = "Client is eager to expand ad spend on TikTok and Meta for the upcoming holiday surge. Wants weekly reporting dashboards.",
                aiSummary = "Sarah reviewed the Q3 performance (+34% ROAS) and approved increasing monthly spend to $25k. Agency agreed to deliver updated creative batches by Friday and set up live Looker Studio reporting.",
                transcript = "Agent: Hi Sarah, thanks for joining the Q4 strategy call.\nClient: Hey team! We were thrilled with last month's ROAS numbers.\nAgent: We're seeing great traction with the short-form UGC videos. We recommend scaling the budget by 30% for October.\nClient: That aligns with our board goals. Let's do $25k/mo starting Oct 1st. Can you send the creative specs by Friday?\nAgent: Absolutely. We'll deliver 8 net-new video concepts and configure the real-time reporting dashboard.",
                transcriptSegmentsJson = """[{"speaker":"Agency Agent","timestampMs":0,"text":"Hi Sarah, thanks for joining the Q4 strategy call."},{"speaker":"Sarah Jenkins","timestampMs":14000,"text":"Hey team! We were thrilled with last month's ROAS numbers."},{"speaker":"Agency Agent","timestampMs":32000,"text":"We're seeing great traction with short-form UGC videos. We recommend scaling budget by 30%."},{"speaker":"Sarah Jenkins","timestampMs":55000,"text":"That aligns with our board goals. Let's do $25k/mo starting Oct 1st. Can you send creative specs by Friday?"},{"speaker":"Agency Agent","timestampMs":88000,"text":"Absolutely. We will deliver 8 net-new video concepts and configure real-time reporting."}]""",
                keyTakeawaysJson = """["Approved $25k/mo ad spend budget for Q4 starting Oct 1st","UGC short-form creative generated 34% ROAS improvement in Q3","Client requested automated live Looker dashboard","Target audience expansion towards B2B founders"]""",
                actionItemsJson = """[{"id":"1","task":"Send 8 new UGC creative briefs & scripts","assignee":"Creative Director","dueDate":"Friday, 5 PM","isCompleted":false,"priority":"High"},{"id":"2","task":"Deploy real-time Looker Studio reporting link","assignee":"Analytics Lead","dueDate":"Monday","isCompleted":true,"priority":"Medium"},{"id":"3","task":"Update contract addendum with approved $25k/mo budget","assignee":"Account Executive","dueDate":"Tomorrow","isCompleted":false,"priority":"High"}]""",
                bookmarksJson = """[{"id":"b1","timestampMs":55000,"title":"$25k Budget Approved","category":"Pricing","note":"Client confirmed budget increase"},{"id":"b2","timestampMs":88000,"title":"Creative Deadline Friday","category":"Action Item","note":"Deliver 8 concepts"}]""",
                clientSentiment = com.example.data.model.Sentiment.HIGH_INTENT,
                dealIntentScore = 94,
                dealSizeEstimate = "$25,000 / month",
                keyObjections = "None; primary concern was maintaining CPA efficiency while scaling volume.",
                followUpEmailDraft = "Subject: Apex Growth x Agency - Q4 Strategy Recap & Action Items\n\nHi Sarah,\n\nThank you for today's productive call! As discussed:\n\n1. Budget: Confirmed scale to $25k/month for Q4 starting Oct 1.\n2. Creative Deliverables: We are assembling 8 new UGC concepts by this Friday.\n3. Analytics: Live Looker dashboard access will be delivered by Monday.\n\nLooking forward to an exceptional quarter together!\n\nBest,\nAgency Team",
                waveformAmplitudesJson = """[0.2, 0.45, 0.7, 0.85, 0.6, 0.3, 0.5, 0.9, 0.75, 0.4, 0.65, 0.8, 0.95, 0.55, 0.35, 0.7, 0.85, 0.4, 0.6, 0.75, 0.9, 0.65, 0.45, 0.8, 0.85, 0.3, 0.6, 0.7, 0.85, 0.5]""",
                isTranscribed = true,
                isAnalyzing = false
            )

            val sampleCall2 = CallRecording(
                title = "Nimbus Cloud - Custom Enterprise SLA & Contract Terms",
                clientName = "David Sterling",
                clientCompany = "Nimbus Cloud Systems",
                clientPhone = "+1 (555) 876-5432",
                clientEmail = "d.sterling@nimbuscloud.tech",
                callType = com.example.data.model.CallType.NEGOTIATION,
                direction = com.example.data.model.CallDirection.INBOUND,
                audioFilePath = "",
                durationMs = 915000L, // 15m 15s
                timestamp = System.currentTimeMillis() - 3600000 * 24,
                fileSize = 10400000L,
                isStarred = false,
                notes = "David requested a 24/7 dedicated Slack channel and 99.9% uptime SLA guarantee before signing the annual retainer.",
                aiSummary = "Negotiated terms for annual $14k/mo enterprise retainer. Client requested 24/7 Slack triage and 2-hour emergency turnaround SLA. Agency agreed to include tier-1 support with quarterly performance reviews.",
                transcript = "Agent: Good afternoon David, let's walk through the enterprise agreement.\nClient: We are 90% there, but our VP of Engineering insists on a 2-hour response guarantee for critical alerts.\nAgent: We can include 24/7 emergency response in Tier-1 Enterprise with a dedicated Slack channel.\nClient: Perfect. If you can revise section 4.2 to reflect that, I will route it for signature by Wednesday.",
                transcriptSegmentsJson = """[{"speaker":"Agency Agent","timestampMs":0,"text":"Good afternoon David, let's walk through the enterprise agreement."},{"speaker":"David Sterling","timestampMs":18000,"text":"We are 90% there, but our VP insists on a 2-hour response guarantee."},{"speaker":"Agency Agent","timestampMs":45000,"text":"We can include 24/7 emergency response in Tier-1 Enterprise with dedicated Slack channel."},{"speaker":"David Sterling","timestampMs":72000,"text":"Perfect. If you can revise section 4.2, I will route for signature by Wednesday."}]""",
                keyTakeawaysJson = """["Enterprise contract value at $14,000/mo ($168k/yr)","Required addition of 2-hour critical response SLA","Dedicated Slack channel approved as part of Tier-1 support","Contract signature target: Wednesday"]""",
                actionItemsJson = """[{"id":"n1","task":"Revise Enterprise SLA Section 4.2 to 2-hour turnaround","assignee":"Legal / Account Lead","dueDate":"Tomorrow 12 PM","isCompleted":false,"priority":"High"},{"id":"n2","task":"Send DocuSign envelope to David & VP Engineering","assignee":"Sales Ops","dueDate":"Wednesday","isCompleted":false,"priority":"High"}]""",
                bookmarksJson = """[{"id":"nb1","timestampMs":18000,"title":"SLA 2-hour Requirement","category":"Key Requirement","note":"VP Engineering requirement"},{"id":"nb2","timestampMs":72000,"title":"Signature Commitment","category":"Highlight","note":"Target sign by Wednesday"}]""",
                clientSentiment = com.example.data.model.Sentiment.POSITIVE,
                dealIntentScore = 88,
                dealSizeEstimate = "$14,000 / month",
                keyObjections = "SLA response time for production incidents.",
                followUpEmailDraft = "Subject: Nimbus Cloud x Agency - Revised Enterprise Terms & SLA Agreement\n\nHi David,\n\nGreat speaking with you today. We have updated Section 4.2 of the agreement to include our 24/7 dedicated emergency channel and 2-hour response SLA.\n\nThe revised DocuSign envelope has been sent over for your team's signature.\n\nBest regards,\nAgency Team",
                waveformAmplitudesJson = """[0.3, 0.5, 0.65, 0.4, 0.7, 0.8, 0.55, 0.35, 0.85, 0.9, 0.6, 0.4, 0.75, 0.8, 0.5, 0.65, 0.9, 0.75, 0.3, 0.55, 0.7, 0.85, 0.4, 0.65, 0.8, 0.9, 0.45, 0.3, 0.6, 0.75]""",
                isTranscribed = true,
                isAnalyzing = false
            )

            val sampleCall3 = CallRecording(
                title = "WhatsApp Audio Call - Influencer Campaign Scoping",
                clientName = "Elena Rostova",
                clientCompany = "Vanguard Digital Media",
                clientPhone = "+1 (555) 432-1098",
                clientEmail = "elena@vanguarddigital.co",
                callType = com.example.data.model.CallType.DISCOVERY,
                direction = com.example.data.model.CallDirection.INBOUND,
                platform = "WhatsApp",
                audioSourceUsed = "VOICE_COMMUNICATION (VoIP)",
                audioFilePath = "",
                durationMs = 380000L, // 6m 20s
                timestamp = System.currentTimeMillis() - 3600000 * 4,
                fileSize = 4200000L,
                isStarred = true,
                notes = "Elena reached out via WhatsApp Voice regarding our TikTok creator network. Wants a pilot campaign for their upcoming SaaS product launch.",
                aiSummary = "Elena initiated a WhatsApp voice call to scope a 30-day influencer pilot ($6,200/mo). Target audience is Gen-Z creators in North America. Agency agreed to send creator portfolio and WhatsApp contract link by Thursday.",
                transcript = "Client (Elena): Hey! Glad we could connect quickly on WhatsApp. We need an agile influencer team for our creator platform launch.\nAgent: Great to connect Elena. WhatsApp is perfect for rapid communication. What's your target launch date?\nClient: We're targeting the 15th of next month. Can your agency source 15 vetted TikTok tech creators within our $6k test budget?\nAgent: Yes, we can tap our pre-vetted SaaS creator roster. We will handle creator outreach, contracts, and usage rights.\nClient: Awesome. Message me the creator deck on WhatsApp when ready.",
                transcriptSegmentsJson = """[{"speaker":"Elena Rostova (WhatsApp)","timestampMs":0,"text":"Hey! Glad we could connect quickly on WhatsApp. We need an agile influencer team for our creator platform launch."},{"speaker":"Agency Agent","timestampMs":22000,"text":"Great to connect Elena. WhatsApp is perfect for rapid communication. What's your target launch date?"},{"speaker":"Elena Rostova (WhatsApp)","timestampMs":48000,"text":"We are targeting the 15th of next month. Can your agency source 15 vetted TikTok creators within our $6k test budget?"},{"speaker":"Agency Agent","timestampMs":78000,"text":"Yes, we can tap our pre-vetted roster. We will handle outreach, contracts, and usage rights."},{"speaker":"Elena Rostova (WhatsApp)","timestampMs":105000,"text":"Awesome. Message me the creator deck on WhatsApp when ready."}]""",
                keyTakeawaysJson = """["WhatsApp voice call scoping $6,200 influencer launch pilot","Target: 15 vetted TikTok tech creators launched by 15th next month","Agency handles end-to-end creator agreements and media usage rights","Deliver creator portfolio & proposal via WhatsApp by Thursday"]""",
                actionItemsJson = """[{"id":"wa1","task":"Compile 15 creator profiles matching tech/SaaS niche","assignee":"Influencer Lead","dueDate":"Thursday, 2 PM","isCompleted":false,"priority":"High"},{"id":"wa2","task":"Send proposal deck & agreement link to Elena on WhatsApp","assignee":"Account Manager","dueDate":"Thursday, 5 PM","isCompleted":false,"priority":"High"},{"id":"wa3","task":"Set up dedicated WhatsApp client update channel","assignee":"Operations","dueDate":"Friday","isCompleted":true,"priority":"Medium"}]""",
                bookmarksJson = """[{"id":"wab1","timestampMs":48000,"title":"$6k Test Budget Confirmed","category":"Pricing","note":"Client approved $6,200 pilot budget"},{"id":"wab2","timestampMs":105000,"title":"WhatsApp Creator Deck Follow-Up","category":"Action Item","note":"Send deck directly in WhatsApp chat"}]""",
                clientSentiment = com.example.data.model.Sentiment.HIGH_INTENT,
                dealIntentScore = 92,
                dealSizeEstimate = "$6,200 / month",
                keyObjections = "Ensuring creators deliver before the strict 15th launch deadline.",
                followUpEmailDraft = "Subject: Vanguard Digital x Agency - WhatsApp Call Recap & Creator Pilot Next Steps\n\nHi Elena,\n\nGreat speaking with you over WhatsApp today! Here is our quick action summary:\n\n1. Scope: 15 vetted TikTok tech creators for your launch on the 15th.\n2. Budget: $6,200 pilot test including all licensing and usage rights.\n3. Deliverable: We will share the creator roster with you directly on WhatsApp by Thursday 2 PM.\n\nLooking forward to a high-impact launch!\n\nBest,\nAgency Team",
                waveformAmplitudesJson = """[0.25, 0.4, 0.6, 0.8, 0.7, 0.5, 0.65, 0.9, 0.75, 0.4, 0.55, 0.8, 0.9, 0.6, 0.35, 0.7, 0.85, 0.5, 0.6, 0.75, 0.95, 0.7, 0.45, 0.8, 0.85, 0.4, 0.65, 0.7, 0.85, 0.6]""",
                isTranscribed = true,
                isAnalyzing = false
            )

            callDao.insertRecording(sampleCall1)
            callDao.insertRecording(sampleCall2)
            callDao.insertRecording(sampleCall3)
        }
    }
}
