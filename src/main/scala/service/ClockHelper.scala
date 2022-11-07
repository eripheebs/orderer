package service

import java.time.LocalDateTime

trait Clock {
	def now(): LocalDateTime  
}

class DefaultClock extends Clock {
	def now(): LocalDateTime = LocalDateTime.now()
}
