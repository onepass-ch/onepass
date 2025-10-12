package ch.onepass.onepass.ui.myevents

import androidx.annotation.ColorRes
import ch.onepass.onepass.R

enum class TicketStatus(@ColorRes val colorRes: Int) {
  CURRENTLY(R.color.status_currently),
  UPCOMING(R.color.status_upcoming),
  EXPIRED(R.color.status_expired)
}
