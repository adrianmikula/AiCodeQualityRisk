package com.aicodequalityrisk.plugin.analysis

import com.aicodequalityrisk.plugin.model.AnalysisInput
import com.aicodequalityrisk.plugin.model.RiskResult

interface AnalyzerClient {
    fun analyze(input: AnalysisInput): RiskResult
}
