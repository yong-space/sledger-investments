import { useState, useEffect } from 'react';
import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import Typography from '@mui/material/Typography';
import Container from '@mui/material/Container';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Autocomplete from '@mui/material/Autocomplete';
import TextField from '@mui/material/TextField';
import Snackbar from '@mui/material/Snackbar';
import SnackbarContent from '@mui/material/SnackbarContent';
import MenuItem from '@mui/material/MenuItem';
import './portfolio.css';

const darkTheme = createTheme({
    palette: { mode: 'dark' },
});

const debounce = (func, timeout = 300) => {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => { func.apply(this, args); }, timeout);
    };
};

const fields = [
    { field: 'symbol', label: "Symbol", sortable: true },
    { field: 'name', label: "Name", sortable: true },
    { field: 'position', label: "Position", sortable: false, decimals: 0 },
    { field: 'price', label: "Price", sortable: false, decimals: 3 },
    { field: 'dividends', label: "Dividends", sortable: true, colour: true, decimals: 2 },
    { field: 'changeToday', label: "Today P&L", sortable: true, colour: true, decimals: 2 },
    { field: 'changeTodayPercentage', label: "Today %", sortable: true, colour: true, decimals: 1 },
    { field: 'profit', label: "P&L", sortable: true, colour: true, decimals: 2 },
    { field: 'profitPercentage', label: "P&L %", sortable: true, colour: true, decimals: 1 },
];

const sort = (data, sortField, sortAsc) => {
    data.sort((a, b) => a[sortField] > b[sortField] ? (sortAsc ? 1 : -1) : (sortAsc ? -1 : 1));
    return data;
}

const PortTable = ({ label, data }) => {
    const [ viewData, setViewData ] = useState(data);
    const [ sortField, setSortField ] = useState('changeTodayPercentage');
    const [ sortAsc, setSortAsc ] = useState(false);
    const sortData = (newSortField) => {
        let order = sortAsc;
        if (newSortField === sortField) {
            order = !order;
            setSortAsc(order);
        } else {
            setSortField(newSortField);
        }
        setViewData(sort([ ...data ], newSortField, order));
    };

    const HeaderTableCell = ({ field, label, sortable }) => {
        return (
            <TableCell
                key={field}
                onClick={() => { sortable ? sortData(field) : void(0) }}
                className={sortable ? 'select' : ''}
            >
                {label}
                { (sortField === field) && <Box sx={{ float: 'right'}}>{ sortAsc ? '▲' : '▼' }</Box> }
            </TableCell>
        )
    };

    const formatNumber = (value, decimals) => {
        const parts = value.toFixed(decimals).split(".");
        const whole = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
        return (parts[1] && decimals > 0) ? `${whole}.${parts[1]}` : whole;
    };

    const DataTableCell = ({ value, decimals, colour }) => {
        const decimalsDefined = parseInt(decimals) % 1 === 0;
        const formatted = (!decimalsDefined || !value) ? value : formatNumber(value, decimals);
        return (
            <TableCell
                align={ decimalsDefined ? 'right' : 'left' }
                className={ colour ? (value > 0 ? 'green' : value < 0 ? 'red' : '') : '' }
            >
                {formatted}
            </TableCell>
        );
    };

    const SummaryRow = ({ data }) => {
        const amount = data.map(i => i.amount).reduce((a, b) => a += b);
        const changeToday = data.map(i => i['changeToday']).reduce((a, b) => a += b);
        const changeTodayPercentage = changeToday * 100 / amount;
        const profit = data.map(i => i['profit']).reduce((a, b) => a += b);
        const profitPercentage = profit * 100 / amount;

        return (
            <TableRow>
                <TableCell colSpan={5}></TableCell>
                <DataTableCell value={changeToday} {...fields[5]} />
                <DataTableCell value={changeTodayPercentage} {...fields[6]} />
                <DataTableCell value={profit} {...fields[7]} />
                <DataTableCell value={profitPercentage} {...fields[8]} />
            </TableRow>
        );
    }

    return (
        <>
            <Typography variant="h5">{label} Portfolio</Typography>
            <TableContainer component={Paper} sx={{ my: "1rem" }}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            { fields.map((field) => <HeaderTableCell key={field.field} {...field} />) }
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {viewData.map((row) => (
                            <TableRow key={row.symbol}>
                                { fields.map((field) => <DataTableCell key={field.field} value={row[field.field]} {...field} />) }
                            </TableRow>
                        ))}
                        <SummaryRow data={viewData} />
                    </TableBody>
                </Table>
            </TableContainer>
        </>
    );
};

const InstrumentPicker = ({ api, instrument, setInstrument }) => {
    const [options, setOptions] = useState([]);
    const search = debounce((event, inputValue) => {
        if (inputValue.trim().length === 0) {
            setOptions([]);
            return;
        }
        api(`/search?query=${inputValue.trim()}`, null, (response) => setOptions(response['Data']));
    }, 300);

    return (
        <Autocomplete
            filterOptions={(x) => x}
            options={options}
            value={instrument}
            onInputChange={search}
            onChange={(event, newValue) => setInstrument(newValue)}
            renderInput={(params) => <TextField fullWidth required {...params} name="instrument" size="small" label="Instrument" />}
            getOptionLabel={(option) => `${option.Symbol} ${option['Description']}`}
        />
    );
};

const AddTransactionForm = ({ api, refreshData, setSnackbar }) => {
    const [ txType, setTxType ] = useState('Trade');
    const [ instrument, setInstrument ] = useState();
    const addTrade = (e) => {
        e.preventDefault();
        const values = Object.values(e.target)
            .filter(field => field.name)
            .reduce((obj,field) => {
                obj[field.name] = field.type === 'number' ? field.valueAsNumber :
                    field.type === 'date' ? field.valueAsDate.toISOString() : field.value;
                return obj;
            }, {});
        values.instrument = instrument?.Identifier;
        values.ticker = instrument?.Symbol;

        api('/add-tx', values, () => {
            setSnackbar({ open: true, error: false, msg: 'Trade Added' });
            setInstrument(null);
            e.target.reset();
            refreshData();
        });
    };
    const inputProps = { step: 'any' };

    const FormFields = () => {
        const instrumentAuto = <InstrumentPicker key="instrumentPicker" {...{ api, instrument, setInstrument }} />;
        const instrumentText = <TextField key="instrumentText" fullWidth required size="small" name="instrument" label="Instrument" />;
        const price = <TextField key="price" fullWidth required size="small" type="number" inputProps={inputProps} name="price" label="Price" />;
        const quantity = <TextField key="quantity" fullWidth required size="small" type="number" inputProps={inputProps} name="quantity" label="Quantity" />;
        const notional = <TextField key="notional" fullWidth required size="small" type="number" inputProps={inputProps} name="notional" label="Notional" />;

        switch (txType) {
            case "Trade": return [ instrumentAuto, price, quantity, notional ];
            case "Deposit": return [ price, notional ];
            case "Dividends": return [ instrumentAuto, notional ];
            case "Fees":
            case "Interest": return [ instrumentText, notional ];
        }
    };

    return (
        <form onSubmit={addTrade}>
            <Box sx={{ display: 'inline-flex', width: '30rem', maxWidth: '100%', flexDirection: "column", gap: '1rem', my: 4 }}>
                <Typography variant="h5">Add Transaction</Typography>
                <TextField select name="type" size="small" label="Transaction Type" value={txType} onChange={(e) => setTxType(e.target.value)}>
                    <MenuItem value="Trade">Trade</MenuItem>
                    <MenuItem value="Deposit">Deposit</MenuItem>
                    <MenuItem value="Dividends">Dividends</MenuItem>
                    <MenuItem value="Fees">Fees</MenuItem>
                    <MenuItem value="Interest">Interest</MenuItem>
                </TextField>
                <TextField fullWidth required size="small" type="date" name="date" label="Date" defaultValue={new Date().toISOString().substring(0, 10)} />
                <FormFields />
                <Button type="submit" variant="contained">Add Transaction</Button>
            </Box>
        </form>
    );
};

const Portfolio = () => {
    const [ snackbar, setSnackbar ] = useState({ open: false });
    const [ data, setData ] = useState();
    const snackbarStyle = { color: 'white', fontWeight: 'bold' };

    const baseUri = window.location.hostname === 'localhost' ? 'http://localhost:8080' : '';
    const api = (uri, body, callback) => {
        const config = body && {
            method: "post",
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        };
        fetch(baseUri + uri, config)
            .then(async (response) => {
                if (response.ok) {
                    return response?.json() || response?.text();
                } else {
                    throw await response.json();
                }
            })
            .then(callback)
            .catch((response) => {
                const { status, error, message } = response;
                if (message.indexOf('401 Unauthorized') === 0) {
                    window.location.href = '/authorize';
                    return;
                }
                setSnackbar({ open: true, error: true, msg: `Error ${status||''} ${error||''}: ${message}` });
                console.log(response);
            });
    }

    const refreshData = () => api('/portfolio', null, (response) => {
        const us = sort(response.filter(i => i.symbol.indexOf(':') > -1 && !i.symbol.endsWith(':xses')), 'changeTodayPercentage', false);
        const sg = sort(response.filter(i => i.symbol.endsWith(':xses')), 'changeTodayPercentage', false);
        const crypto = sort(response.filter(i => i.symbol.indexOf(':') === -1), 'changeTodayPercentage', false);
        setData({ us, sg, crypto });
    });

    useEffect(refreshData, []);

    return (
        <ThemeProvider theme={darkTheme}>
            { !data ? (<div className="loading"></div>) : (
                <Container>
                    <CssBaseline />
                    <Box sx={{ my: 4 }}>
                        { data.sg.length > 0 ? <PortTable label="SG" data={data.sg} /> : '' }
                        { data.us.length > 0 ? <PortTable label="US" data={data.us} /> : '' }
                        { data.crypto.length > 0 ? <PortTable label="Crypto" data={data.crypto} /> : '' }
                    </Box>
                    <AddTransactionForm {...{ api, setSnackbar, refreshData }} />
                </Container>
            )}
            <Snackbar
                open={snackbar.open}
                onClose={() => setSnackbar({ open: false })}
                autoHideDuration={snackbar.error ? 10000 : 3000}
            >
                <SnackbarContent style={{ ...snackbarStyle, backgroundColor: snackbar.error ? '#d74545' : '#4e9a51' }} message={snackbar.msg} />
            </Snackbar>
        </ThemeProvider>
    );
};

export default Portfolio;
