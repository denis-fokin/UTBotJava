package org.utbot.predictors

import org.utbot.analytics.NNStateRewardPredictor
import org.utbot.framework.UtSettings
import smile.math.MathEx.dot
import smile.math.matrix.Matrix
import java.io.File

private const val DEFAULT_WEIGHT_PATH = "linear.txt"

/**
 * Last weight is bias
 */
private fun loadWeights(path: String): Matrix {
    val weightsFile = File("${UtSettings.rewardModelPath}/${path}")
    val weightsArray = weightsFile.readText().splitByCommaIntoDoubleArray()
    return Matrix(weightsArray)
}

class LinearStateRewardPredictor(weightsPath: String = DEFAULT_WEIGHT_PATH, scalerPath: String = DEFAULT_SCALER_PATH) :
    NNStateRewardPredictor {
    private val weights = loadWeights(weightsPath)
    private val scaler = loadScaler(scalerPath)

    fun predict(input: List<List<Double>>): List<Double> {
        // add 1 to each feature vector
        val matrixValues = input
            .map { (it + 1.0).toDoubleArray() }
            .toTypedArray()

        val X = Matrix(matrixValues)

        return X.mm(weights).col(0).toList()
    }

    override fun predict(input: List<Double>): Double {
        var inputArray =  Matrix(input.toDoubleArray()).sub(scaler.mean).div(scaler.variance).col(0)
        inputArray += 1.0

        return dot(inputArray, weights.col(0))
    }
}