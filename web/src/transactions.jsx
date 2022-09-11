import { useState, useEffect } from 'react';
import AddTransactionForm from './add-tx-form';
import DataGrid from './datagrid';
import Api from './api';
import Loader from './Loader';

const fields = [
    { field: "id", label: "ID", sortable: true },
    { field: "date", label: "Date", sortable: true, date: true },
    { field: "type", label: "Type", sortable: true },
    { field: "ticker", label: "Ticker", sortable: true },
    { field: "quantity", label: "Quantity", sortable: true, colour: true, decimals: 0 },
    { field: "price", label: "Price", sortable: true, decimals: 3 },
    { field: "amount", label: "Amount", sortable: true, colour: true, decimals: 2 },
    { field: "delete", label: "Delete", type: 'deleteButton' }
];

const Transactions = () => {
    const { listTx } = Api();
    const [ data, setData ] = useState();

    const refreshData = () => listTx((response) => setData(response));
    useEffect(refreshData, []);

    return (
        !data ? <Loader /> : <>
            <AddTransactionForm {...{ refreshData }} />
            <DataGrid
                label="Transactions"
                defaultSortField="date"
                {...{ data, setData, fields }}
            />
        </>
    );
};
export default Transactions;
