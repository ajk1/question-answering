import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import rank.CompositeRanker;
import rank.IRanker;
import rank.NgramRanker;
import rank.OtherRanker;
import type.Measurement;
import type.Passage;
import type.Question;

/**
 * This CAS Consumer generates the report file with the method metrics
 */
public class PassageRankingWriter extends CasConsumer_ImplBase {
  final String PARAM_OUTPUTDIR = "OutputDir";

  final String OUTPUT_FILENAME = "ErrorAnalysis.csv";

  File mOutputDir;

  IRanker ngramRanker, otherRanker;

  CompositeRanker compositeRanker;

  @Override
  public void initialize() throws ResourceInitializationException {
    String mOutputDirStr = (String) getConfigParameterValue(PARAM_OUTPUTDIR);
    if (mOutputDirStr != null) {
      mOutputDir = new File(mOutputDirStr);
      if (!mOutputDir.exists()) {
        mOutputDir.mkdirs();
      }
    }

    // Initialize rankers
    compositeRanker = new CompositeRanker();
    ngramRanker = new NgramRanker();
    otherRanker = new OtherRanker();
    compositeRanker.addRanker(ngramRanker);
    compositeRanker.addRanker(otherRanker);
  }

  @Override
  public void processCas(CAS arg0) throws ResourceProcessException {
    System.out.println(">> Passage Ranking Writer Processing");
    // Import the CAS as a aJCas
    JCas aJCas = null;
    File outputFile = null;
    PrintWriter writer = null;
    try {
      aJCas = arg0.getJCas();
      try {
        outputFile = new File(Paths.get(mOutputDir.getAbsolutePath(), OUTPUT_FILENAME).toString());
        outputFile.getParentFile().mkdirs();
        writer = new PrintWriter(outputFile);
      } catch (FileNotFoundException e) {
        System.out.printf("Output file could not be written: %s\n",
                Paths.get(mOutputDir.getAbsolutePath(), OUTPUT_FILENAME).toString());
        return;
      }

      writer.println("question_id,tp,fn,fp,precision,recall,f1");
      // Retrieve all the questions for printout
      List<Question> allQuestions = UimaUtils.getAnnotations(aJCas, Question.class);
      List<Question> subsetOfQuestions = RandomUtils.getRandomSubset(allQuestions, 10);

      // TODO: Here one needs to sort the questions in ascending order of their question ID
      for (int i=1; i<10; i++) {
    	  int j = i;
    	  while (j > 0) {
    		  j--;
    		  if (subsetOfQuestions.get(i).getId().compareTo(subsetOfQuestions.get(j).getId()) > 0) {
    			  j++;
    			  break;
    		  }
    	  }
    	  subsetOfQuestions.add(j, subsetOfQuestions.get(i));
    	  subsetOfQuestions.remove(i+1);
      }
      
      for (Question question : subsetOfQuestions) {
        List<Passage> passages = UimaUtils.convertFSListToList(question.getPassages(), Passage.class);
        
        // TODO: Use the following three lists of ranked passages for your error analysis
        List<Passage> ngramRankedPassages = ngramRanker.rank(question, passages);
        List<Passage> otherRankedPassages = otherRanker.rank(question, passages);
        List<Passage> compositeRankedPassages = compositeRanker.rank(question, passages);

        //Calculate measurement and add to question
        Measurement m = new Measurement(aJCas);
        int tp = 0, fp = 0, fn = 0;
        final double SCORE_THRESHOLD = 0.45;
        //ngram threshold .35 - other .6 - composite .45
        
        for (Passage passage : passages) {
        	if (passage.getLabel() == true) {
        		if (passage.getScore() > SCORE_THRESHOLD)
        			tp += 1;
        		else  
        			fn += 1;
        	}
        	else if (passage.getScore() > SCORE_THRESHOLD)  
        		fp += 1;
        }
        m.setTp(tp);
        m.setFp(fp);
        m.setFn(fn);
        question.setMeasurement(m);

        //Measurement m = question.getMeasurement();
        
        // TODO: Calculate actual precision, recall and F1
        double precision = (tp==0 ? 0 : (double) tp / (tp+fp));
        double recall = (double) tp / (tp+fn);
        double f1 = (precision*recall==0 ? 0 : 2*(precision*recall) / (precision+recall));

        writer.printf("%s,%d,%d,%d,%.3f,%.3f,%.3f\n", question.getId(), m.getTp(), m.getFn(),
                m.getFp(), precision, recall, f1);
      }
    } catch (CASException e) {
      try {
        throw new CollectionException(e);
      } catch (CollectionException e1) {
        e1.printStackTrace();
      }
    } finally {
      if (writer != null)
        writer.close();
    }
  }
}
