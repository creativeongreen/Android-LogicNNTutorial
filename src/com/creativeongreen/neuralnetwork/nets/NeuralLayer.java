/*
 * Copyright (C) 2015 creativeongreen
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.creativeongreen.neuralnetwork.nets;

import java.io.Serializable;
import android.util.Log;

import com.creativeongreen.neuralnetwork.activation.ActivationFunction;
import com.creativeongreen.neuralnetwork.util.WeightMatrix;

/**
*
* @author creativeongreen
* 
* Neural layer data structure implementation
* 
*/
public class NeuralLayer implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String LOG_TAG = "NN_NeuralLayer";

	private static int id;
	private int neuronCount;
	private boolean hasBias;

	// output matrix [1 x n] where n are number of nodes of current layer
	private WeightMatrix matrixNeuronOutput;

	// error gradient
	private WeightMatrix matrixNeuronDelta;

	// weight matrix [2 x 3] means previous layer have 2 nodes, and current layer have 3 nodes
	// {w00, w01, w02}
	// {w10, w11, w12}
	private WeightMatrix matrixWeight;

	// changed weight for momentum
	// {cw00, cw01, cw02}
	// {cw10, cw11, cw12}
	private WeightMatrix matrixChangedWeight;

	private ActivationFunction activationFunction;

	public NeuralLayer(final ActivationFunction activationFunction,
			final double bias, final int count, NeuralLayer prevLayer) {
		this.activationFunction = activationFunction;
		this.hasBias = (bias == 1.0) ? true : false;
		int iBias = this.hasBias ? 1 : 0;
		this.neuronCount = count;
		this.matrixNeuronOutput = new WeightMatrix(1, count + iBias);
		// set bias output as 1 if defined
		if (this.hasBias)
			(this.matrixNeuronOutput.getMatrix())[0][this.neuronCount] = 1;
		this.matrixNeuronDelta = new WeightMatrix(1, count + iBias);

		// construct weight matrix
		if (prevLayer != null) {
			iBias = prevLayer.hasBias ? 1 : 0;
			this.matrixWeight = new WeightMatrix(prevLayer.neuronCount + iBias,
					this.neuronCount);
			this.matrixWeight.initialize();
			//this.matrixWeight.print();
			this.matrixChangedWeight = new WeightMatrix(prevLayer.neuronCount
					+ iBias, this.neuronCount);
		}

		/*
		Log.i(LOG_TAG,
				"new a layer-" + NeuralLayer.getId() + "/ # of Neurons: "
						+ "- " + this.neuronCount
						+ ", matrixNeuronOutput: 1 x "
						+ this.matrixNeuronOutput.getColumnCount()
						+ ", matrixNeuronDelta: 1 x "
						+ this.matrixNeuronDelta.getColumnCount());
		*/

		NeuralLayer.setId(NeuralLayer.getId() + 1);
	}

	public WeightMatrix getWeightMatrix() {
		return this.matrixWeight;
	}

	public WeightMatrix getOutputs() {
		return this.matrixNeuronOutput;
	}

	/*
	 * - for input layer copy input as output directly, output[i] = input - for other layers
	 * output[j] = activation( sum( w[i][j] * output[i] ) + bias[i][j] ) where i: # of neurons on
	 * previous layer, j: # of neurons on current layer
	 */
	public void computeOutputs(Object object) {
		if (object instanceof double[]) {
			// this is an input layer
			double[] input = double[].class.cast(object);
			if (input.length != this.neuronCount)
				throw new RuntimeException(
						"computeOutputs: input dimensions not match number of neurons on input layer.");
			this.matrixNeuronOutput.setWeightMatrix(input);
		}

		else {
			// this is the hidden layer or output layer
			NeuralLayer prevLayer = NeuralLayer.class.cast(object);
			WeightMatrix sumWeights = prevLayer.matrixNeuronOutput
					.times(this.matrixWeight);
			for (int j = 0; j < this.neuronCount; j++)
				(this.matrixNeuronOutput.getMatrix())[0][j] = this.activationFunction
						.activate((sumWeights.getMatrix())[0][j]);
		}
	}

	/*
	 * compute error gradient - for output layer delta[k] = ( expected[k] - output[k] ) *
	 * derivative(output[k]) - for others layers delta[j] = sum( w[j][k] * delta[k] ) *
	 * derivative(output[j])
	 */
	public void computeLayerDeltas(Object object) {
		if (object instanceof double[]) {
			// compute output layer deltas
			double[] expected = double[].class.cast(object);
			for (int k = 0; k < this.neuronCount; k++) {
				double outputK = (this.matrixNeuronOutput.getMatrix())[0][k];
				(matrixNeuronDelta.getMatrix())[0][k] = (expected[k] - outputK)
						* this.activationFunction.derivative(outputK);
			}

		} else {
			// compute hidden layer deltas
			NeuralLayer nextLayer = NeuralLayer.class.cast(object);
			int jBias = this.hasBias ? 1 : 0;
			int kBias = nextLayer.hasBias ? 1 : 0;
			double sumDeltaWeight = 0;

			for (int j = 0; j < this.neuronCount + jBias; j++) {
				for (int k = 0; k < nextLayer.neuronCount + kBias; k++) {
					double toNextLayerNeuronWeight = (nextLayer.matrixWeight
							.getMatrix())[j][k];
					double nextLayerNeuronDelta = (nextLayer.matrixNeuronDelta
							.getMatrix())[0][k];
					sumDeltaWeight += (toNextLayerNeuronWeight * nextLayerNeuronDelta);
				}

				double outputJ = (this.matrixNeuronOutput.getMatrix())[0][j];
				(matrixNeuronDelta.getMatrix())[0][j] = sumDeltaWeight
						* this.activationFunction.derivative(outputJ);
			}
		}
	}

	/*
	 * w(t+1)[j][k] = w(t)[j][k] + delta_w(t)[j][k] + momentum * delta_w(t-1)[j][k]* where
	 * delta_w(t)[j][k] = learning_rate * output(t)[j] * delta(t)[k]
	 */
	public void updateWeights(NeuralLayer prevLayer, double learningRate,
			double momentum) {
		int jBias = prevLayer.hasBias ? 1 : 0;

		for (int k = 0; k < this.neuronCount; k++) {
			for (int j = 0; j < prevLayer.neuronCount + jBias; j++) {
				double prevLayerOutput = (prevLayer.matrixNeuronOutput
						.getMatrix())[0][j];
				double deltaWeight = learningRate * prevLayerOutput
						* (this.matrixNeuronDelta.getMatrix())[0][k];
				(this.matrixWeight.getMatrix())[j][k] += (deltaWeight + momentum
						* (this.matrixChangedWeight.getMatrix())[j][k]);
				// keep current deltaWeight in order for being used on next momentum computation
				(this.matrixChangedWeight.getMatrix())[j][k] = deltaWeight;
			}
		}
	}

	public double computeTrainingError(double[] expected) {
		double sumError = 0;
		for (int k = 0; k < this.neuronCount; k++) {
			double offset = expected[k]
					- (this.matrixNeuronOutput.getMatrix())[0][k];
			sumError += Math.pow(offset, 2);
		}

		return sumError / 2.0;
	}

	public static int getId() {
		return id;
	}

	public static void setId(int id) {
		NeuralLayer.id = id;
	}
}
