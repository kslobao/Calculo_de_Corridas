package com.calculocorridas.services.accessibility.parsers

import com.calculocorridas.selectors.PatternMatcher
import javax.inject.Inject

class NinetyNineParser @Inject constructor(matcher: PatternMatcher) : BaseParser(matcher) {
    override val appKey = "99"
}
