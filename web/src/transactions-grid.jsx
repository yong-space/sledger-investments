import { useEffect, useState } from 'react';
import Api from './api';

const TransactionsGrid = () => {
    const { listTx } = Api();
    const [ txList, setTxList ] = useState();

    useEffect(() => {
        listTx((response) => setTxList(response));
    }, []);

    return (
        <>
            Hello
        </>
    );
};
export default TransactionsGrid;
