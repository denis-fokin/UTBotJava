package org.utbot.analytics

interface NNStateRewardPredictorFactory {
    operator fun invoke(): NNStateRewardPredictor
}