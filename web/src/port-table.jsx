import { useState } from 'react';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

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
export default PortTable;
