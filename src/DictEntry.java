public class DictEntry {

        int df; // Document frequency
        Posting pList; // Pointer to the postings list

        DictEntry() {
            df = 0;
            pList = null;
        }

}
