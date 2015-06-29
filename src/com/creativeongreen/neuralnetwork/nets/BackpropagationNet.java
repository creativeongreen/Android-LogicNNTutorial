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
import java.util.ArrayList;
import android.util.Log;

import com.creativeongreen.neuralnetwork.activation.SigmoidActivation;

/**
*
* @author creativeongreen
* 
* Back-propagation implementation
* 
*/
public class BackpropagationNet implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String LOG_TAG = "NN_BackpropagationNet";

	// private static final double MAX_NUMBER_OF_EPOCH = 200000;
	// private static final double MIN_GLOBAL_ERROR = 0.00001;

	private NeuralLayer inputNeuralLayer;
	private NeuralLayer hiddenNeuralLayer;
	private NeuralLayer outputNeuralLayer;

	private final ArrayList<NeuralLayer> neuralLayers = new ArrayList<NeuralLayer>();

	private double globalError;
	private double learningRate;
	private double momentum;
	private double bias = 1.0;
	private double maxEpoch, epoch;
	private double trainingError = 1;
	private boolean forceStop = false;

	public BackpropagationNet(int numInputNeurons, int numHiddenNeurons,
			int numOutputNeurons, double learningRate, double momentum,
			double epoch, double globalError) {

		inputNeuralLayer = new NeuralLayer(null, bias, numInputNeurons, null);
		neuralLayers.add(inputNeuralLayer);

		hiddenNeuralLayer = new NeuralLayer(new SigmoidActivation(), bias,
				numHiddenNeurons, inputNeuralLayer);
		neuralLayers.add(hiddenNeuralLayer);

		outputNeuralLayer = new NeuralLayer(new SigmoidActivation(), 0.0,
				numOutputNeurons, hiddenNeuralLayer);
		neuralLayers.add(outputNeuralLayer);

		this.learningRate = learningRate;
		this.momentum = momentum;
		this.maxEpoch = epoch;
		this.globalError = globalError;
		this.forceStop = false;
	}

	public void train(double[][] inputTrainingSet,
			double[][] expectedTrainingSet) {

		epoch = 0;
		int indexTrainDataSet = 0;
		double prevTrainingError = 0; // used on data tracking
		while (trainingError > this.globalError && epoch < this.maxEpoch
				&& !forceStop) {

			feedForward(inputTrainingSet[indexTrainDataSet]);
			backPropagation(expectedTrainingSet[indexTrainDataSet]);

			indexTrainDataSet = (indexTrainDataSet + 1)
					% inputTrainingSet.length;

			if (indexTrainDataSet == 0)
				epoch++;

			/*
			 * if (epoch % 2000 == 0) { System.out.printf("epoch= %6.0f, error= %1.6f, %4.2f%%\n",
			 * epoch, trainingError, (trainingError - prevTrainingError) * 100 / prevTrainingError);
			 * prevTrainingError = trainingError; }
			 */

		}

		// System.out.printf("Training finished: epoch= %6.0f, error= %1.6f\n",
		// epoch, trainingError);
		// System.out.println("---------------------------");
	}

	public void feedForward(double[] input) {

		for (int i = 0; i < neuralLayers.size(); i++) {
			neuralLayers.get(i).computeOutputs(
					i == 0 ? input : neuralLayers.get(i - 1));
		}

	}

	public void backPropagation(double[] expected) {

		int outputLayerIndex = neuralLayers.size() - 1;

		for (int i = outputLayerIndex; i > 0; i--) {
			neuralLayers.get(i).computeLayerDeltas(
					i == outputLayerIndex ? expected : neuralLayers.get(i + 1));
			neuralLayers.get(i).updateWeights(neuralLayers.get(i - 1),
					learningRate, momentum);
		}

		trainingError = neuralLayers.get(outputLayerIndex)
				.computeTrainingError(expected);

	}

	public double[] getOutputResults() {
		return (outputNeuralLayer.getOutputs().getMatrix())[0];
	}

	public int getEpoch() {
		return (int) epoch;
	}

	public void stopTraining() {
		forceStop = true;
	}

}
