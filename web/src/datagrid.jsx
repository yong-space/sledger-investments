import { useState, useEffect } from 'react';
import dayjs from 'dayjs';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Util from './util';
import DeleteButton from './delete-button';

const DataGrid = ({ label, data, setData, fields, summary, defaultSortField }) => {
  const [ viewData, setViewData ] = useState();
  const [ sortField, setSortField ] = useState(defaultSortField);
  const [ sortAsc, setSortAsc ] = useState(false);
  const { sort } = Util();

  useEffect(() => {
    setViewData(data);
  }, [ data ]);

  const sortData = (newSortField) => {
    let order = sortAsc;
    if (newSortField === sortField) {
      order = !order;
      setSortAsc(order);
    } else {
      setSortField(newSortField);
    }
    setViewData(sort([...data], newSortField, order));
  };

  const HeaderTableCell = ({ field, label, sortable, type }) => {
    return (
      <TableCell
        key={field}
        onClick={() => sortable && sortData(field)}
        className={sortable ? "select" : ""}
        sx={{ width: type ? '7rem' : 'auto' }}
      >
        {label}
        {sortField === field && (
          <Box sx={{ float: "right" }}>{sortAsc ? "▲" : "▼"}</Box>
        )}
      </TableCell>
    );
  };

  const formatNumber = (value, decimals) => {
    const parts = value.toFixed(decimals).split(".");
    const whole = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    return parts[1] && decimals > 0 ? `${whole}.${parts[1]}` : whole;
  };

  const renderValue = (row, type, promise, formatted) => {
      if (!type) {
        return formatted;
      }
      const renderMap = {
        deleteButton: <DeleteButton id={row.id} {...{ setData, promise }} />,
      }
      return renderMap[type] || formatted;
  };

  const DataTableCell = ({ row, type, value, decimals, colour, date, promise }) => {
    const decimalsDefined = parseInt(decimals) % 1 === 0;
    let formatted = value;
    if (value && decimalsDefined) {
      formatted = formatNumber(value, decimals);
    } else if (date) {
      formatted = dayjs(value).format('YYYY-MM-DD')
    }

    return (
      <TableCell
        align={decimalsDefined ? "right" : "left"}
        className={colour ? (value > 0 ? "green" : value < 0 ? "red" : "") : ""}
      >
        { renderValue(row, type, promise, formatted) }
      </TableCell>
    );
  };

  const SummaryRow = ({ skip, sFields }) => (
    <TableRow>
      { skip > 0 && <TableCell colSpan={skip}></TableCell> }
      { sFields.map((field) => <DataTableCell key={field.field} {...field} />)}
    </TableRow>
  );

  return (
    <>
      <Typography variant="h5">{label}</Typography>
      <TableContainer component={Paper} sx={{ my: "1rem" }}>
        <Table size="small">
          <TableHead>
            <TableRow>
              {fields.map((field) => (
                <HeaderTableCell key={field.field} {...field} />
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {!viewData ?
              <TableRow><DataTableCell value="No Data" /></TableRow> :
              viewData.map((row, index) => (
              <TableRow key={index}>
                {fields.map((field) => (
                  <DataTableCell
                    key={field.field}
                    value={row[field.field]}
                    row={row}
                    {...field}
                  />
                ))}
              </TableRow>
            ))}
            { summary && <SummaryRow {...summary } /> }
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};
export default DataGrid;
