import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool m_utxoPool;

    public TxHandler(UTXOPool utxoPool) {
        m_utxoPool = new UTXOPool(utxoPool);
    }

    public boolean isValidTx(Transaction tx) {
        double totInputs = 0, totOutputs = 0;
        Set<UTXO> utxoSet = new HashSet<UTXO>();
        // validate inputs
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for(int i=0 ; i<inputs.size() ; ++i) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            // is this utxo used twice in the same transaction?
            if(!utxoSet.add(utxo)) {
                return false;
            }
            // make sure it exists in the UTXO pool
            if(!m_utxoPool.contains(utxo)) {
                return false;
            }
            Transaction.Output prev_out = m_utxoPool.getTxOutput(utxo);
            // is the signature valid?
            if(!Crypto.verifySignature(prev_out.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }
            totInputs += prev_out.value;
        }
       
        // validate outputs
        for(int i=0 ; i<tx.getOutputs().size() ; ++i){
            Transaction.Output output = tx.getOutput(i);
            if(output.value < 0) {
                return false;
            }
            totOutputs += output.value;
        }
       
        if(totInputs < totOutputs) {
            return false;
        }
       
        return true;
    }

    public Transaction[] handleTxs(Transaction[] possibleTxs) {
       
        ArrayList<Transaction> validTrxs = new ArrayList<>();
        for (int i = 0; i < possibleTxs.length; i++) {
            // first make sure the transaction is valid
            if(isValidTx(possibleTxs[i])){
                validTrxs.add(possibleTxs[i]);
                updateUTXOPool(possibleTxs[i]);
            }
        }
       
        return validTrxs.toArray(new Transaction[]{});
    }
   
    private void updateUTXOPool(Transaction tx){
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        // add new unsent outputs to the pool
        for (int i = 0; i < outputs.size(); i++) {
            UTXO utxo = new UTXO(tx.getHash(), i);
            m_utxoPool.addUTXO(utxo, tx.getOutput(i));
        }
       
        // remove outputs that are now spent
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        for(int j=0 ; j<inputs.size() ; ++j){
            Transaction.Input input = tx.getInput(j);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            m_utxoPool.removeUTXO(utxo);
        }
    }
}
