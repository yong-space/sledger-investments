import Typography from '@mui/material/Typography';
import AddTransactionForm from './add-tx-form';
import TransactionsGrid from './transactions-grid';

const Transactions = () => {
    return (
        <>
            <Typography variant="h5">Transactions</Typography>
            <TransactionsGrid />
            <AddTransactionForm />
        </>
    );
};
export default Transactions;
