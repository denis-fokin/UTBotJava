package org.utbot.predictors

import org.utbot.analytics.NNStateRewardPredictorFactory
import org.utbot.framework.NNStateRewardPredictorType
import org.utbot.framework.UtSettings

class NNStateRewardPredictorFactoryImpl : NNStateRewardPredictorFactory {
    override operator fun invoke() = when (UtSettings.nnStateRewardPredictorType) {
        NNStateRewardPredictorType.BASE -> NNStateRewardPredictorSmile()
        NNStateRewardPredictorType.TORCH -> NNStateRewardPredictorTorch()
        NNStateRewardPredictorType.LINEAR -> LinearStateRewardPredictor()
    }
}