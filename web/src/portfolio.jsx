import { useState, useEffect } from 'react';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import PortTable from './port-table';
import AddTransactionForm from './add-tx-form';
import Api from './api';

const sort = (data, sortField, sortAsc) => {
    data.sort((a, b) => a[sortField] > b[sortField] ? (sortAsc ? 1 : -1) : (sortAsc ? -1 : 1));
    return data;
};

const Portfolio = () => {
    const [ data, setData ] = useState();
    const { getPortfolio } = Api();

    const refreshData = () => getPortfolio((response) => {
        const us = sort(response.filter(i => i.symbol.indexOf(':') > -1 && !i.symbol.endsWith(':xses')), 'changeTodayPercentage', false);
        const sg = sort(response.filter(i => i.symbol.endsWith(':xses')), 'changeTodayPercentage', false);
        const crypto = sort(response.filter(i => i.symbol.indexOf(':') === -1), 'changeTodayPercentage', false);
        setData({ us, sg, crypto });
    });

    useEffect(refreshData, []);

    return !data ? <div className="loading"></div> : (
        <Container>
            <Box sx={{ my: 4 }}>
                { data.sg.length > 0 ? <PortTable label="SG" data={data.sg} /> : '' }
                { data.us.length > 0 ? <PortTable label="US" data={data.us} /> : '' }
                { data.crypto.length > 0 ? <PortTable label="Crypto" data={data.crypto} /> : '' }
            </Box>
            <AddTransactionForm {...{ refreshData }} />
        </Container>
    );
};

export default Portfolio;
