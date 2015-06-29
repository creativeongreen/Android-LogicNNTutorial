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

package com.creativeongreen.neuralnetwork.util;

import java.util.ArrayList;

/**
*
* @author creativeongreen
* 
* Neural weight manipulation
* 
*/
public class WeightMatrix {

	// java denote: matrix 3 x 4
	// double[][] = {
	// 		{w00, w01, w02, w03}
	// 		{w10, w11, w12, w13}
	// 		{w20, w21, w22, w23}
	// }
	private double[][] matrix; // row x column array

	public WeightMatrix(int row, int column) {
		this.matrix = new double[row][column];
	}

	public WeightMatrix(double[] data) {
		this.matrix = new double[1][data.length];
		for (int i = 0; i < data.length; i++) {
			this.matrix[0][i] = data[i];
		}
	}

	public WeightMatrix(double[][] data) {
		this.matrix = new double[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				this.matrix[i][j] = data[i][j];
			}
		}
	}

	public WeightMatrix clone() {
		return new WeightMatrix(this.matrix);
	}

	public void initialize() {
		// final double range = max - min;
		// randomNumber = (range * Math.random()) + min;
		for (int i = 0; i < this.getRowCount(); i++)
			for (int j = 0; j < this.getColumnCount(); j++)
				//this.matrix[i][j] = Math.random(); // 0.0 ~ 1.0
				this.matrix[i][j] = 2.0 * Math.random() - 1.0; // -1.0 ~ 1.0
	}

	public double[][] getMatrix() {
		return this.matrix;
	}

	// create n x n identity matrix
	public static WeightMatrix identity(int n) {
		WeightMatrix I = new WeightMatrix(n, n);
		for (int i = 0; i < n; i++)
			I.matrix[i][i] = 1;
		return I;
	}

	public WeightMatrix transpose() {
		WeightMatrix T = new WeightMatrix(this.getColumnCount(),
				this.getRowCount());
		for (int i = 0; i < this.getRowCount(); i++)
			for (int j = 0; j < this.getColumnCount(); j++)
				T.matrix[j][i] = this.matrix[i][j];

		return T;
	}

	public WeightMatrix plus(WeightMatrix B) {
		WeightMatrix A = this;
		if (B.getRowCount() != A.getRowCount()
				|| B.getColumnCount() != A.getColumnCount())
			throw new RuntimeException("plus: Illegal matrix dimensions.");

		WeightMatrix C = new WeightMatrix(getRowCount(), getColumnCount());
		for (int i = 0; i < getRowCount(); i++)
			for (int j = 0; j < getColumnCount(); j++)
				C.matrix[i][j] = A.matrix[i][j] + B.matrix[i][j];

		return C;
	}

	public WeightMatrix minus(WeightMatrix B) {
		WeightMatrix A = this;
		if (B.getRowCount() != A.getRowCount()
				|| B.getColumnCount() != A.getColumnCount())
			throw new RuntimeException("plus: Illegal matrix dimensions.");

		WeightMatrix C = new WeightMatrix(getRowCount(), getColumnCount());
		for (int i = 0; i < getRowCount(); i++)
			for (int j = 0; j < getColumnCount(); j++)
				C.matrix[i][j] = A.matrix[i][j] - B.matrix[i][j];

		return C;
	}

	public WeightMatrix times(WeightMatrix B) {
		WeightMatrix A = this;
		if (A.getColumnCount() != B.getRowCount())
			throw new RuntimeException("times: Illegal matrix dimensions.");

		WeightMatrix C = new WeightMatrix(A.getRowCount(), B.getColumnCount());
		for (int i = 0; i < C.getRowCount(); i++)
			for (int j = 0; j < C.getColumnCount(); j++)
				for (int k = 0; k < A.getColumnCount(); k++)
					C.matrix[i][j] += (A.matrix[i][k] * B.matrix[k][j]);
		return C;
	}

	public boolean equals(WeightMatrix B) {
		WeightMatrix A = this;
		if (B.getRowCount() != A.getRowCount()
				|| B.getColumnCount() != A.getColumnCount())
			throw new RuntimeException("equals: Illegal matrix dimensions.");

		for (int i = 0; i < getRowCount(); i++)
			for (int j = 0; j < getColumnCount(); j++)
				if (A.matrix[i][j] != B.matrix[i][j])
					return false;

		return true;
	}

	public void setWeightMatrix(double[] data) {
		// System.arraycopy(data, 0, this.matrix[0], 0, data.length);
		for (int i = 0; i < data.length; i++) {
			this.matrix[0][i] = data[i];
		}
	}

	// { {w00}, {w10}, {w20} } -> { {w00, w01, w02} }
	public WeightMatrix createRowWeightMatrix(double[] data) {
		double[][] rowMatrix = new double[1][data.length];
		System.arraycopy(data, 0, rowMatrix[0], 0, data.length);
		// for (int i = 0; i < data.length; i++) rowMatrix[0][i] = data[i];
		return new WeightMatrix(rowMatrix);
	}

	// { {w00, w01, w02} } -> { {w00}, {w10}, {w20} }
	public WeightMatrix createColumnWeightMatrix(double[] data) {
		final double[][] columnMatrix = new double[data.length][1];
		for (int i = 0; i < data.length; i++)
			columnMatrix[i][0] = data[i];
		return new WeightMatrix(columnMatrix);
	}

	public WeightMatrix getRow(final int row) {
		if (row > getRowCount()) {
			throw new RuntimeException("Can't get row #" + row
					+ " because it does not exist.");
		}

		final double[][] newMatrix = new double[1][getColumnCount()];

		for (int col = 0; col < getColumnCount(); col++) {
			newMatrix[0][col] = this.matrix[row][col];
		}

		return new WeightMatrix(newMatrix);
	}

	public ArrayList<double[]> getRows() {
		ArrayList<double[]> resultMatrix = new ArrayList<double[]>();

		for (int i = 0; i < getRowCount(); i++) {
			resultMatrix.add(this.matrix[i]);
		}

		return resultMatrix;
	}

	public int getRowCount() {
		return this.matrix.length;
	}

	public int getColumnCount() {
		return this.matrix[0].length;
	}

	public void print() {
		System.out.printf("dimensions: %d x %d", this.getRowCount(),
				this.getColumnCount());
		System.out.println();
		for (int i = 0; i < this.getRowCount(); i++) {
			for (int j = 0; j < this.getColumnCount(); j++)
				System.out.printf("%1.4f ", this.matrix[i][j]);
			System.out.println();
		}
	}

}
