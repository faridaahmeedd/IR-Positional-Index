import java.util.ArrayList;
import java.util.List;

public class Posting {
    int docId; // Document ID
    int dtf; // Document term frequency
    double tfidf; // TF-IDF value
    int tf;
    Posting next; // Pointer to the next posting
    List<Integer> positions;

    Posting() {
        docId = 0;
        dtf = 0;
//        tf =0 ;
        tfidf = 0.0;
        this.positions = new ArrayList<>();
        next = null;
    }
}