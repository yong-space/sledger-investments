import { useState } from 'react';
import { useRecoilState } from 'recoil';
import { atoms } from './atoms';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import InstrumentPicker from './instrument-picker';
import Api from './api';

const AddTransactionForm = ({ refreshData }) => {
    const [ , setStatus ] = useRecoilState(atoms.status);
    const [ txType, setTxType ] = useState('Trade');
    const [ instrument, setInstrument ] = useState();
    const { addTx } = Api();

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

        addTx(values, () => {
            setStatus({ open: true, error: false, msg: 'Trade Added' });
            setInstrument(null);
            e.target.reset();
            refreshData();
        });
    };
    const inputProps = { step: 'any' };

    const FormFields = () => {
        const instrumentAuto = <InstrumentPicker key="instrumentPicker" {...{ instrument, setInstrument }} />;
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

export default AddTransactionForm;
