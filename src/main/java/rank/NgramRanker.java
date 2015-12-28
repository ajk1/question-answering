package rank;

import java.util.*;

import type.Passage;
import type.Question;

public class NgramRanker extends AbstractRanker {

  /**
   * Returns a score of the given passage associated with the given question.
   * 
   * @param question
   * @param passage
   * @return a score of the passage
   */
  @Override
  public Double score(Question question, Passage passage) {
    // TODO Complete the implementation of this method.\
	String[] qTokens = question.getSentence().toLowerCase().split("[^a-zA-Z]+");
	String[] pTokens = passage.getText().toLowerCase().split("[^a-zA-Z]+");
	
	ArrayList<String> terms = new ArrayList<String>();
	ArrayList<Integer> questionTF = new ArrayList<Integer>(); //term frequency of each term in question
	ArrayList<Integer> passageTF = new ArrayList<Integer>(); //term frequency of each term in passage
	
	//finds list of terms and each term frequency within question and passage
	for (String s : qTokens) {
		int index = terms.indexOf(s);
		if (index >= 0) {
			questionTF.set(index, new Integer(1 + questionTF.get(index)));
		}
		else {
			terms.add(s);
			questionTF.add(new Integer(1));
			passageTF.add(new Integer(0));
		}
	}
	for (String s : pTokens) {
		int index = terms.indexOf(s);
		if (index >= 0) {
			passageTF.set(index, new Integer(1 + passageTF.get(index)));
		}
	}
	
	ArrayList<Double> idf = new ArrayList<Double>();//inverse document frequency
	ArrayList<Double> questionWeights = new ArrayList<Double>();
	ArrayList<Double> passageWeights = new ArrayList<Double>();
	double qVectorLength = 0;
	double pVectorLength = 0;
	double qDotP = 0;
	for (int i=0; i<terms.size(); i++) {
		idf.add(new Double(Math.log(10/
				(passageTF.get(i).intValue() + questionTF.get(i).intValue()))));
		double qWeight = idf.get(i).doubleValue() * questionTF.get(i).intValue();
		double pWeight = idf.get(i).doubleValue() * passageTF.get(i).intValue();
		questionWeights.add(new Double(qWeight));
		passageWeights.add(new Double(pWeight));
		qVectorLength += Math.pow(qWeight, 2);
		pVectorLength += Math.pow(pWeight, 2);
		qDotP += qWeight*pWeight;
	}
	
	qVectorLength = Math.pow(qVectorLength, .5);
	pVectorLength = Math.pow(pVectorLength, .5);
	
	double cosineSimilarity = qDotP/(qVectorLength*pVectorLength);
	passage.setScore(cosineSimilarity);
    return cosineSimilarity;
  }

}
